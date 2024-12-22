/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package com.redhat.devtools.lsp4ij.features.formatting;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.containers.ContainerUtil;
import com.redhat.devtools.lsp4ij.LanguageServerItem;
import com.redhat.devtools.lsp4ij.LanguageServiceAccessor;
import com.redhat.devtools.lsp4ij.features.codeBlockProvider.LSPCodeBlockProvider;
import com.redhat.devtools.lsp4ij.features.codeBlockProvider.LSPCodeBlockUtils;
import com.redhat.devtools.lsp4ij.features.completion.LSPTypedHandlerDelegate;
import com.redhat.devtools.lsp4ij.features.selectionRange.LSPSelectionRangeSupport;
import com.redhat.devtools.lsp4ij.server.definition.LanguageServerDefinition;
import com.redhat.devtools.lsp4ij.server.definition.launching.ClientConfigurationSettings;
import com.redhat.devtools.lsp4ij.server.definition.launching.ClientConfigurationSettings.ClientConfigurationFormatSettings;
import com.redhat.devtools.lsp4ij.server.definition.launching.ClientConfigurationSettings.ClientConfigurationFormatSettings.ClientConfigurationFormatScope;
import com.redhat.devtools.lsp4ij.server.definition.launching.UserDefinedLanguageServerDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.redhat.devtools.lsp4ij.internal.CompletableFutures.isDoneNormally;
import static com.redhat.devtools.lsp4ij.internal.CompletableFutures.waitUntilDone;

/**
 * Typed handler for LSP4IJ-managed files that performs automatic on-type formatting for specific keystrokes.
 */
public class LSPClientSideOnTypeFormattingTypedHandler extends TypedHandlerDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(LSPClientSideOnTypeFormattingTypedHandler.class);

    @Override
    @NotNull
    public Result charTyped(char c,
                            @NotNull Project project,
                            @NotNull Editor editor,
                            @NotNull PsiFile file) {
        ClientConfigurationSettings clientConfigurationSettings = getClientConfigurationSettings(file);
        if (clientConfigurationSettings != null) {
            ClientConfigurationFormatSettings formatSettings = clientConfigurationSettings.format;

            // Close braces
            if (formatSettings.formatOnCloseBrace) {
                Map.Entry<Character, Character> bracePair = ContainerUtil.find(
                        LSPCodeBlockUtils.getBracePairs(file).entrySet(),
                        entry -> entry.getValue() == c
                );
                if (bracePair != null) {
                    Character openBraceChar = bracePair.getKey();
                    Character closeBraceChar = bracePair.getValue();
                    if (StringUtil.isEmpty(formatSettings.formatOnCloseBraceCharacters) ||
                        formatSettings.formatOnCloseBraceCharacters.contains(String.valueOf(closeBraceChar))) {
                        return handleCloseBraceTyped(
                                project,
                                editor,
                                file,
                                formatSettings,
                                openBraceChar,
                                closeBraceChar
                        );
                    }
                }
            }

            // Statement terminators
            // TODO: Need client config for whether or not this should be enabled? Also which exact characters are
            //  considered statement terminators in the language
            if (c == ';') {
                return handleStatementTerminatorTyped(project, editor, file);
            }

            // Completion triggers
            // TODO: Need client config for whether or not this should be enabled?
            if (LSPTypedHandlerDelegate.hasLanguageServerSupportingCompletionTriggerCharacters(c, project, file)) {
                return handleCompletionTriggerTyped(project, editor, file);
            }
        }

        return super.charTyped(c, project, editor, file);
    }

    @Nullable
    private static ClientConfigurationSettings getClientConfigurationSettings(@NotNull PsiFile file) {
        List<LanguageServerItem> languageServers = getLanguageServers(file);
        // TODO: What would it mean to support multiple language servers here?
        LanguageServerItem languageServer = ContainerUtil.getFirstItem(languageServers);
        LanguageServerDefinition serverDefinition = languageServer != null ? languageServer.getServerDefinition() : null;
        if (serverDefinition instanceof UserDefinedLanguageServerDefinition languageServerDefinition) {
            return languageServerDefinition.getLanguageServerClientConfiguration();
        }
        return null;
    }

    @NotNull
    private static List<LanguageServerItem> getLanguageServers(@NotNull PsiFile file) {
        List<LanguageServerItem> languageServers = new LinkedList<>();

        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile != null) {
            Project project = file.getProject();
            CompletableFuture<List<LanguageServerItem>> languageServersFuture = LanguageServiceAccessor.getInstance(project).getLanguageServers(
                    virtualFile,
                    clientFeatures -> clientFeatures.getFormattingFeature().isEnabled(file),
                    clientFeatures -> clientFeatures.getFormattingFeature().isSupported(file)
            );
            try {
                waitUntilDone(languageServersFuture, file);
            } catch (ProcessCanceledException e) {
                //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
                //TODO delete block when minimum required version is 2024.2
                return languageServers;
            } catch (CancellationException e) {
                // cancel the LSP requests textDocument/selectionRanges
                return languageServers;
            } catch (ExecutionException e) {
                LOGGER.error("Error while finding language servers for file '{}'", virtualFile.getPath(), e);
                return languageServers;
            }

            if (!isDoneNormally(languageServersFuture)) {
                return languageServers;
            }

            ContainerUtil.addAllNotNull(languageServers, languageServersFuture.getNow(Collections.emptyList()));
        }

        return languageServers;
    }

    @NotNull
    private static Result handleCloseBraceTyped(@NotNull Project project,
                                                @NotNull Editor editor,
                                                @NotNull PsiFile file,
                                                @NotNull ClientConfigurationFormatSettings formatSettings,
                                                char openBraceChar,
                                                char closeBraceChar) {
        // Statement-level scope is not supported for code blocks
        if (formatSettings.formatOnCloseBraceScope == ClientConfigurationFormatScope.STATEMENT) {
            return Result.CONTINUE;
        }

        // If configured to format the entire file, do so now
        TextRange fileTextRange = file.getTextRange();
        if (formatSettings.formatOnCloseBraceScope == ClientConfigurationFormatScope.FILE) {
            CodeStyleManager.getInstance(project).reformatText(file, Collections.singletonList(fileTextRange));
            return Result.STOP;
        }

        // Otherwise find the code block that was closed by the brace
        int offset = editor.getCaretModel().getOffset();
        int beforeOffset = offset - 1;
        TextRange codeBlockRange = LSPCodeBlockProvider.getCodeBlockRange(editor, file, beforeOffset);
        if (codeBlockRange != null) {
            int startOffset = codeBlockRange.getStartOffset();
            int endOffset = codeBlockRange.getEndOffset();

            // Make sure the range includes the brace pair
            Document document = editor.getDocument();
            CharSequence documentChars = document.getCharsSequence();
            if ((startOffset > 0) && (documentChars.charAt(startOffset) != openBraceChar)) {
                startOffset--;
            }
            if ((endOffset < (documentChars.length() - 1)) && (documentChars.charAt(endOffset) != closeBraceChar)) {
                endOffset++;
            }

            // If the range is now the braced block, format it
            if ((documentChars.charAt(startOffset) == openBraceChar) && (documentChars.charAt(endOffset) == closeBraceChar)) {
                CodeStyleManager.getInstance(project).reformatText(file, startOffset, endOffset);
                return Result.STOP;
            }
        }

        // If we couldn't find a code block and we're configured to degrade gracefully to the file, do so now
        else if (formatSettings.formatOnCloseBraceDegradeToFile) {
            CodeStyleManager.getInstance(project).reformatText(file, Collections.singletonList(fileTextRange));
            return Result.STOP;
        }

        return Result.CONTINUE;
    }

    @NotNull
    private static Result handleStatementTerminatorTyped(@NotNull Project project,
                                                         @NotNull Editor editor,
                                                         @NotNull PsiFile file) {
        // Find the statement that was just terminated
        int offset = editor.getCaretModel().getOffset();
        int beforeOffset = offset - 1;
        List<TextRange> selectionTextRanges = LSPSelectionRangeSupport.getSelectionTextRanges(file, editor, beforeOffset);
        if (!ContainerUtil.isEmpty(selectionTextRanges)) {
            // Find the closest selection range that is extended to line start/end; that should be the statement
            Document document = editor.getDocument();
            CharSequence documentChars = document.getCharsSequence();
            TextRange statementSelectionTextRange = ContainerUtil.find(
                    selectionTextRanges,
                    selectionTextRange -> {
                        int startOffset = selectionTextRange.getStartOffset();
                        int endOffset = selectionTextRange.getEndOffset();

                        // Remove leading/trailing newlines from the range
                        while ((startOffset < endOffset) && (documentChars.charAt(startOffset) == '\n')) startOffset++;
                        while ((endOffset > startOffset) && (documentChars.charAt(endOffset - 1) == '\n')) endOffset--;

                        // See if this is a selection of complete lines
                        int startLineNumber = document.getLineNumber(startOffset);
                        int startLineStartOffset = document.getLineStartOffset(startLineNumber);
                        if (startLineStartOffset == startOffset) {
                            int endLineNumber = document.getLineNumber(endOffset);
                            int endLineEndOffset = document.getLineEndOffset(endLineNumber);
                            if ((endLineEndOffset == endOffset) || (endLineEndOffset == (endOffset + 1))) {
                                // Make sure that it ends with the terminator that was just typed
                                String selectionRangeText = StringUtil.trimTrailing(documentChars.subSequence(startOffset, endOffset).toString());
                                return selectionRangeText.endsWith(";") && ((startOffset + selectionRangeText.length()) == offset);
                            }
                        }
                        return false;
                    }
            );

            // If none was found, find the closest selection range that includes the terminator that was just typed
            if (statementSelectionTextRange == null) {
                statementSelectionTextRange = ContainerUtil.find(
                        selectionTextRanges,
                        selectionTextRange -> selectionTextRange.containsOffset(offset)
                );
            }

            // If we found the statement text range, format it
            if (statementSelectionTextRange != null) {
                int startOffset = statementSelectionTextRange.getStartOffset();
                int endOffset = statementSelectionTextRange.getEndOffset();
                CodeStyleManager.getInstance(project).reformatText(file, startOffset, endOffset);
                return Result.STOP;
            }
        }

        return Result.CONTINUE;
    }

    @NotNull
    private static Result handleCompletionTriggerTyped(@NotNull Project project,
                                                       @NotNull Editor editor,
                                                       @NotNull PsiFile file) {
        // Just format the completion trigger
        int offset = editor.getCaretModel().getOffset();
        // TODO: We don't know the length of the completion trigger, but this seems to do the right thing
        int beforeOffset = offset - 1;
        CodeStyleManager.getInstance(project).reformatText(file, beforeOffset, offset);
        return Result.STOP;
    }
}
