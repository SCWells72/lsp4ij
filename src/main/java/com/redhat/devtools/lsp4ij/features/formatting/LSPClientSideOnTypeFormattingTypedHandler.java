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

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.containers.ContainerUtil;
import com.redhat.devtools.lsp4ij.LanguageServiceAccessor;
import com.redhat.devtools.lsp4ij.features.codeBlockProvider.LSPCodeBlockProvider;
import com.redhat.devtools.lsp4ij.features.selectionRange.LSPSelectionRangeSupport;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Typed handler for LSP4IJ-managed files that performs automatic on-type formatting for specific keystrokes.
 */
public class LSPClientSideOnTypeFormattingTypedHandler extends TypedHandlerDelegate {

    @Override
    @NotNull
    public Result charTyped(char c,
                            @NotNull Project project,
                            @NotNull Editor editor,
                            @NotNull PsiFile file) {
        VirtualFile virtualFile = file.getVirtualFile();
        if ((virtualFile != null) && LanguageServiceAccessor.getInstance(project).hasAny(
                virtualFile,
                ls -> ls.getClientFeatures().getFormattingFeature().isSupported(file))
        ) {
            // Respect the IDE-wide setting
            // TODO: Should this be client config instead/also?
            if (CodeInsightSettings.getInstance().REFORMAT_BLOCK_ON_RBRACE && (c == '}')) {
                return handleCloseBraceTyped(project, editor, file);
            }
            // TODO: Need client config for whether or not this should be enabled?
            else if (c == ';') {
                return handleStatementTerminatorTyped(project, editor, file);
            }
        }

        return super.charTyped(c, project, editor, file);
    }

    @NotNull
    private static Result handleCloseBraceTyped(@NotNull Project project,
                                                @NotNull Editor editor,
                                                @NotNull PsiFile file) {
        // Find the code block that was closed by the brace
        int offset = editor.getCaretModel().getOffset();
        int beforeOffset = offset - 1;
        TextRange codeBlockRange = LSPCodeBlockProvider.getCodeBlockRange(editor, file, beforeOffset);
        if (codeBlockRange != null) {
            int startOffset = codeBlockRange.getStartOffset();
            int endOffset = codeBlockRange.getEndOffset();

            // Make sure the range includes the brace pair
            Document document = editor.getDocument();
            CharSequence documentChars = document.getCharsSequence();
            if ((startOffset > 0) && (documentChars.charAt(startOffset) != '{')) {
                startOffset--;
            }
            if ((endOffset < (documentChars.length() - 1)) && (documentChars.charAt(endOffset) != '}')) {
                endOffset++;
            }

            // If the range is now the braced block, format it
            if ((documentChars.charAt(startOffset) == '{') && (documentChars.charAt(endOffset) == '}')) {
                CodeStyleManager.getInstance(project).reformatText(file, startOffset, endOffset);
                return Result.STOP;
            }
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
}
