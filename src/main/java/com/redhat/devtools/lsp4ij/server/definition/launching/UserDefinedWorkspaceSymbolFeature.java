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

import com.redhat.devtools.lsp4ij.client.features.LSPWorkspaceSymbolFeature;
import com.redhat.devtools.lsp4ij.server.definition.ClientConfigurableLanguageServerDefinition;

/**
 * Adds client-side workspace symbol configuration features.
 */
public class UserDefinedWorkspaceSymbolFeature extends LSPWorkspaceSymbolFeature {

    @Override
    public boolean supportsGotoClass() {
        ClientConfigurableLanguageServerDefinition serverDefinition = (ClientConfigurableLanguageServerDefinition) getClientFeatures().getServerDefinition();
        ClientConfigurationSettings clientConfiguration = serverDefinition.getLanguageServerClientConfiguration();
        return clientConfiguration != null ? clientConfiguration.workspaceSymbol.supportsGotoClass : super.supportsGotoClass();
    }
}