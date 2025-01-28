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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

class LSPDocumentSymbolViewProvider extends SingleRootFileViewProvider {

    LSPDocumentSymbolViewProvider(@NotNull PsiManager manager,
                                  @NotNull VirtualFile virtualFile,
                                  boolean eventSystemEnabled,
                                  @NotNull Language language) {
        super(manager, virtualFile, eventSystemEnabled, language);
    }

    @Override
    public PsiElement findElementAt(int offset) {
        PsiFile file = ContainerUtil.getFirstItem(getAllFiles());
        return file != null ? LSPDocumentSymbolUtils.getDocumentSymbolData(file, offset) : null;
    }

    @Override
    public PsiElement findElementAt(int offset, @NotNull Class<? extends Language> lang) {
        return findElementAt(offset);
    }

    @Override
    public PsiElement findElementAt(int offset, @NotNull Language language) {
        return findElementAt(offset);
    }
}
