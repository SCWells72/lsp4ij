/*******************************************************************************
 * Copyright (c) 2024 Red Hat Inc. and others.
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
package com.redhat.devtools.lsp4ij.server.definition.launching;

import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.client.features.LSPCompletionFeature;
import org.jetbrains.annotations.NotNull;

/**
 * Adds client-side completion configuration features.
 */
public class UserDefinedCompletionFeature extends LSPCompletionFeature {

    @Override
    public boolean isCaseSensitive(@NotNull PsiFile file) {
        UserDefinedLanguageServerDefinition serverDefinition = (UserDefinedLanguageServerDefinition) getClientFeatures().getServerDefinition();
        ClientConfigurationSettings clientConfiguration = serverDefinition.getLanguageServerClientConfiguration();
        return clientConfiguration != null ? clientConfiguration.completions.caseSensitive : super.isCaseSensitive(file);
    }
}
