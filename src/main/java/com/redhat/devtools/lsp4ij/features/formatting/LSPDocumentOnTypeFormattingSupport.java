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

import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.redhat.devtools.lsp4ij.LSPRequestConstants;
import com.redhat.devtools.lsp4ij.LanguageServerItem;
import com.redhat.devtools.lsp4ij.features.AbstractLSPDocumentFeatureSupport;
import com.redhat.devtools.lsp4ij.internal.CancellationSupport;
import com.redhat.devtools.lsp4ij.internal.CompletableFutures;
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams;
import org.eclipse.lsp4j.TextEdit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * LSP document on-type formatting support which loads and caches selection ranges by consuming:
 *
 * <ul>
 *     <li>LSP 'textDocument/onTypeFormatting' requests</li>
 * </ul>
 */
public class LSPDocumentOnTypeFormattingSupport extends AbstractLSPDocumentFeatureSupport<DocumentOnTypeFormattingParams, List<TextEdit>> {

    public LSPDocumentOnTypeFormattingSupport(@NotNull PsiFile file) {
        super(file);
    }

    public CompletableFuture<List<TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams params) {
        return super.getFeatureData(params);
    }

    @Override
    protected CompletableFuture<List<TextEdit>> doLoad(DocumentOnTypeFormattingParams params, CancellationSupport cancellationSupport) {
        PsiFile file = super.getFile();
        return onTypeFormatting(file, params, cancellationSupport);
    }

    private static @NotNull CompletableFuture<List<TextEdit>> onTypeFormatting(@NotNull PsiFile file,
                                                                               @NotNull DocumentOnTypeFormattingParams params,
                                                                               @NotNull CancellationSupport cancellationSupport) {

        return getLanguageServers(file,
                f -> f.getDocumentOnTypeFormattingFeature().isEnabled(file),
                f -> f.getDocumentOnTypeFormattingFeature().isSupported(file))
                .thenComposeAsync(languageServers -> {
                    // Here languageServers is the list of language servers which matches the given file
                    // and which have selection range capability
                    if (languageServers.isEmpty()) {
                        return CompletableFuture.completedStage(Collections.emptyList());
                    }

                    // Collect list of textDocument/onTypeFormatting future for each language servers
                    List<CompletableFuture<List<TextEdit>>> textEditsPerServerFutures = languageServers
                            .stream()
                            .map(languageServer -> onTypeFormattingFor(params, languageServer, cancellationSupport))
                            .toList();

                    // Merge list of textDocument/onTypeFormatting future in one future which return the list of selection ranges
                    return CompletableFutures.mergeInOneFuture(textEditsPerServerFutures, cancellationSupport);
                });
    }

    private static CompletableFuture<List<TextEdit>> onTypeFormattingFor(DocumentOnTypeFormattingParams params,
                                                                         LanguageServerItem languageServer,
                                                                         CancellationSupport cancellationSupport) {
        return cancellationSupport.execute(
                        languageServer.getTextDocumentService().onTypeFormatting(params),
                        languageServer,
                        LSPRequestConstants.TEXT_DOCUMENT_ON_TYPE_FORMATTING)
                .thenApplyAsync(textEditResults -> {
                    if (textEditResults == null) {
                        // textDocument/textEdit may return null
                        return Collections.emptyList();
                    }
                    // We have to do this to make generics happy due to "? extends TextEdit" returned by onTypeFormatting()
                    List<TextEdit> textEdits = new ArrayList<>(textEditResults.size());
                    ContainerUtil.addAllNotNull(textEdits, textEditResults);
                    return textEdits;
                });
    }

}
