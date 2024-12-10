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
package com.redhat.devtools.lsp4ij.client.features;

import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.server.capabilities.DocumentOnTypeFormattingCapabilityRegistry;
import org.eclipse.lsp4j.ServerCapabilities;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * LSP document on-type formatting feature.
 * <p>
 * Additional information is available on <a href="https://github.com/redhat-developer/lsp4ij/blob/main/docs/LSPApi.md#lsp-on-type-formatting-feature">GitHub</a>
 */
@ApiStatus.Experimental
public class LSPDocumentOnTypeFormattingFeature extends AbstractLSPDocumentFeature {

    private DocumentOnTypeFormattingCapabilityRegistry documentOnTypeFormattingCapabilityRegistry;

    @Override
    public boolean isSupported(@NotNull PsiFile file) {
        return isDocumentOnTypeFormattingSupported(file);
    }

    /**
     * Returns true if the file associated with a language server can support document on-type formatting and false otherwise.
     *
     * @param file the file.
     * @return true if the file associated with a language server can support document on-type formatting and false otherwise.
     */
    public boolean isDocumentOnTypeFormattingSupported(@NotNull PsiFile file) {
        return getDocumentOnTypeFormattingCapabilityRegistry().isDocumentOnTypeFormattingSupported(file);
    }

    public DocumentOnTypeFormattingCapabilityRegistry getDocumentOnTypeFormattingCapabilityRegistry() {
        if (documentOnTypeFormattingCapabilityRegistry == null) {
            initDocumentOnTypeFormattingCapabilityRegistry();
        }
        return documentOnTypeFormattingCapabilityRegistry;
    }

    private synchronized void initDocumentOnTypeFormattingCapabilityRegistry() {
        if (documentOnTypeFormattingCapabilityRegistry != null) {
            return;
        }
        var clientFeatures = getClientFeatures();
        documentOnTypeFormattingCapabilityRegistry = new DocumentOnTypeFormattingCapabilityRegistry(clientFeatures);
        documentOnTypeFormattingCapabilityRegistry.setServerCapabilities(clientFeatures.getServerWrapper().getServerCapabilitiesSync());
    }

    @Override
    public void setServerCapabilities(@Nullable ServerCapabilities serverCapabilities) {
        if (documentOnTypeFormattingCapabilityRegistry != null) {
            documentOnTypeFormattingCapabilityRegistry.setServerCapabilities(serverCapabilities);
        }
    }
}
