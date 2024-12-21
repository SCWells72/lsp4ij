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
package com.redhat.devtools.lsp4ij.features.selectionRange;

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.LanguageServiceAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Implementation of the IDE's extendWordSelectionHandler EP for LSP4IJ files against textDocument/selectionRange.
 */
public class LSPExtendWordSelectionHandler implements ExtendWordSelectionHandler {

    @Override
    public boolean canSelect(@NotNull PsiElement element) {
        if (!element.isValid()) {
            return false;
        }

        PsiFile file = element.getContainingFile();
        if ((file == null) || !file.isValid()) {
            return false;
        }

        Project project = file.getProject();
        if (project.isDisposed()) {
            return false;
        }

        // Only if textDocument/selectionRange is supported for the file
        return LanguageServiceAccessor.getInstance(project)
                .hasAny(file.getVirtualFile(), ls -> ls.getClientFeatures().getSelectionRangeFeature().isSelectionRangeSupported(file));
    }

    @Override
    @Nullable
    public List<TextRange> select(@NotNull PsiElement element,
                                  @NotNull CharSequence editorText,
                                  int offset,
                                  @NotNull Editor editor) {
        PsiFile file = element.getContainingFile();
        if (file == null) {
            return null;
        }

        return LSPSelectionRangeSupport.getSelectionTextRanges(file, editor, offset);
    }
}
