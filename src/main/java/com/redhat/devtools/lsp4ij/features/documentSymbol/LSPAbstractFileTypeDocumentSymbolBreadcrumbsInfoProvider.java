/*******************************************************************************
 * Copyright (c) 2025 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/

package com.redhat.devtools.lsp4ij.features.documentSymbol;

import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.LSPIJEditorUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Document symbol-based breadcrumbs info provider for abstract file type plain text files.
 */
public class LSPAbstractFileTypeDocumentSymbolBreadcrumbsInfoProvider extends AbstractLSPDocumentSymbolBreadcrumbsInfoProvider {

    public LSPAbstractFileTypeDocumentSymbolBreadcrumbsInfoProvider() {
        super(PlainTextLanguage.INSTANCE);
    }

    @Override
    protected boolean isSupported(@NotNull PsiElement element) {
        PsiFile file = element.getContainingFile();
        if (!LSPIJEditorUtils.isSupportedAbstractFileTypeFile(file)) {
            return false;
        }

        return super.isSupported(element);
    }
}
