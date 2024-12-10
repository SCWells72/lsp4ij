/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.lsp4ij.features.formatting;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.redhat.devtools.lsp4ij.LSPFileSupport;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.LanguageServiceAccessor;
import com.redhat.devtools.lsp4ij.client.ExecuteLSPFeatureStatus;
import com.redhat.devtools.lsp4ij.client.indexing.ProjectIndexingManager;
import org.eclipse.lsp4j.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.redhat.devtools.lsp4ij.internal.CompletableFutures.isDoneNormally;
import static com.redhat.devtools.lsp4ij.internal.CompletableFutures.waitUntilDone;

/**
 * Provides on-type formatting via the LSP 'textDocument/onTypeFormatting' feature when available and configured to do so.
 */
public class LSPOnTypeFormattingTypedHandler extends TypedHandlerDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(LSPOnTypeFormattingTypedHandler.class);

    @Override
    public @NotNull Result charTyped(char charTyped,
                                     @NotNull Project project,
                                     @NotNull Editor editor,
                                     @NotNull PsiFile file) {
        if ((charTyped == '}') && CodeInsightSettings.getInstance().REFORMAT_BLOCK_ON_RBRACE) {
            VirtualFile virtualFile = file.getVirtualFile();
            if ((virtualFile != null) &&
                LanguageServiceAccessor.getInstance(project).hasAny(
                        virtualFile,
                        ls -> ls.getClientFeatures().getDocumentOnTypeFormattingFeature().isDocumentOnTypeFormattingSupported(file))
            ) {
                List<TextEdit> textEdits = getTextEdits(charTyped, editor, file);
                if (!ContainerUtil.isEmpty(textEdits)) {
                    LSPIJUtils.applyEdits(editor.getDocument(), textEdits);
                }
            }
        }

        return super.charTyped(charTyped, project, editor, file);
    }

    @NotNull
    private static List<TextEdit> getTextEdits(char charTyped,
                                               @NotNull Editor editor,
                                               @NotNull PsiFile file) {
        if (ProjectIndexingManager.canExecuteLSPFeature(file) != ExecuteLSPFeatureStatus.NOW) {
            return Collections.emptyList();
        }

        LSPDocumentOnTypeFormattingSupport onTypeFormattingSupport = LSPFileSupport.getSupport(file).getDocumentOnTypeFormattingSupport();
        TextDocumentIdentifier textDocumentIdentifier = LSPIJUtils.toTextDocumentIdentifier(file.getVirtualFile());
        Position position = LSPIJUtils.toPosition(editor.getCaretModel().getOffset(), editor.getDocument());
        DocumentOnTypeFormattingParams params = new DocumentOnTypeFormattingParams(textDocumentIdentifier, new FormattingOptions(), position, String.valueOf(charTyped));

        CompletableFuture<List<TextEdit>> textEditFutures = onTypeFormattingSupport.onTypeFormatting(params);
        try {
            waitUntilDone(textEditFutures, file);
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
            //TODO delete block when minimum required version is 2024.2
            onTypeFormattingSupport.cancel();
            return Collections.emptyList();
        } catch (CancellationException e) {
            // cancel the LSP requests textDocument/selectionRanges
            onTypeFormattingSupport.cancel();
            return Collections.emptyList();
        } catch (ExecutionException e) {
            LOGGER.error("Error while consuming LSP 'textDocument/selectionRanges' request", e);
            return Collections.emptyList();
        }

        if (!isDoneNormally(textEditFutures)) {
            return Collections.emptyList();
        }

        return textEditFutures.getNow(Collections.emptyList());
    }
}
