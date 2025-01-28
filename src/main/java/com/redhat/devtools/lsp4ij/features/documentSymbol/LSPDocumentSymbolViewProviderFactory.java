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

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.FileViewProviderFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SingleRootFileViewProvider;
import com.redhat.devtools.lsp4ij.LanguageServersRegistry;
import org.jetbrains.annotations.NotNull;

public class LSPDocumentSymbolViewProviderFactory implements FileViewProviderFactory {
    @Override
    @NotNull
    public FileViewProvider createFileViewProvider(@NotNull VirtualFile virtualFile,
                                                   Language language,
                                                   @NotNull PsiManager manager,
                                                   boolean eventSystemEnabled) {
        Project project = manager.getProject();
        if (LanguageServersRegistry.getInstance().isFileSupported(virtualFile, project)) {
            return new LSPDocumentSymbolViewProvider(manager, virtualFile, eventSystemEnabled, language);
        }

        return new SingleRootFileViewProvider(manager, virtualFile, eventSystemEnabled);
    }
}
