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

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider;
import com.intellij.util.containers.ContainerUtil;
import com.redhat.devtools.lsp4ij.features.documentSymbol.LSPDocumentSymbolStructureViewModel.LSPDocumentSymbolViewElement;
import com.redhat.devtools.lsp4ij.features.documentSymbol.LSPDocumentSymbolStructureViewModel.LSPFileStructureViewElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class that can be extended for custom language servers to add document symbol-based breadcrumbs.
 * Concrete subclasses must have a no-arg constructor that specifies the language(s) for the language server's
 * files and must be registered as a <code>breadcrumbsInfoProvider</code> in <code>plugin.xml</code>.
 */
public abstract class AbstractLSPDocumentSymbolBreadcrumbsInfoProvider implements BreadcrumbsProvider {

    private final Language[] languages;

    /**
     * Creates the document symbol-based breadcrumbs info provider for the specified language(s).
     * @param languages the language(s)
     */
    protected AbstractLSPDocumentSymbolBreadcrumbsInfoProvider(@NotNull Language... languages) {
        this.languages = languages;
    }

    @Override
    public Language[] getLanguages() {
        return languages;
    }

    @Override
    public boolean acceptElement(@NotNull PsiElement element) {
        return element instanceof DocumentSymbolData;
    }

    @Override
    @Nullable
    public PsiElement getParent(@NotNull PsiElement element) {
        if (element instanceof PsiFile file) {
            return file.getParent();
        } else if (element instanceof DocumentSymbolData documentSymbolData) {
            return documentSymbolData.getParent();
        } else {
            return LSPDocumentSymbolUtils.getDocumentSymbolData(element);
        }
    }

    @Override
    @NotNull
    public List<PsiElement> getChildren(@NotNull PsiElement element) {
        if (element instanceof PsiFile file) {
            LSPDocumentSymbolStructureViewModel structureViewModel = LSPDocumentSymbolUtils.getStructureViewModel(file);
            StructureViewTreeElement root = structureViewModel != null ? structureViewModel.getRoot() : null;
            if (root instanceof LSPFileStructureViewElement fileStructureViewElement) {
                StructureViewTreeElement[] children = fileStructureViewElement.getChildren();
                List<PsiElement> childElements = new ArrayList<>(children.length);
                for (StructureViewTreeElement child : children) {
                    if (child instanceof LSPDocumentSymbolViewElement documentSymbolViewElement) {
                        ContainerUtil.addIfNotNull(childElements, documentSymbolViewElement.getElement());
                    }
                }
                return childElements;
            }
        } else {
            DocumentSymbolData documentSymbolData = LSPDocumentSymbolUtils.getDocumentSymbolData(element);
            if (documentSymbolData != null) {
                return Arrays.asList(documentSymbolData.getChildren());
            }
        }

        return Collections.emptyList();
    }

    @Override
    @NotNull
    public String getElementInfo(@NotNull PsiElement element) {
        if (element instanceof PsiFile file) {
            return file.getName();
        } else {
            DocumentSymbolData documentSymbolData = LSPDocumentSymbolUtils.getDocumentSymbolData(element);
            if (documentSymbolData != null) {
                String presentableText = documentSymbolData.getPresentableText();
                String name = documentSymbolData.getName();
                return presentableText != null ? presentableText : name != null ? name : "";
            }
        }

        return "";
    }

    @Override
    @Nullable
    public String getElementTooltip(@NotNull PsiElement element) {
        if (element instanceof PsiFile) return null;
        return getElementInfo(element);
    }

    @Override
    @Nullable
    public Icon getElementIcon(@NotNull PsiElement element) {
        if (element instanceof PsiFile) return null;
        DocumentSymbolData documentSymbolData = LSPDocumentSymbolUtils.getDocumentSymbolData(element);
        return documentSymbolData != null ? documentSymbolData.getIcon(false) : null;
    }
}
