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

import org.jetbrains.annotations.NotNull;

/**
 * Client-side settings for a user-defined language server configuration.
 */
public class ClientConfigurationSettings {
    /**
     * Client-side format settings.
     */
    public static class ClientConfigurationFormatSettings {
        /**
         * Supported formatting scopes.
         */
        public enum ClientConfigurationFormatScope {
            /**
             * The current statement if one can be identified.
             */
            STATEMENT,
            /**
             * The current code block if one can be identified.
             */
            CODE_BLOCK,
            /**
             * The current file.
             */
            FILE
        }

        /**
         * Whether or not to format on close brace. Defaults to false.
         */
        public boolean formatOnCloseBrace = false;

        /**
         * The specific close brace characters that should trigger a format. Defaults to the language's close brace
         * characters.
         */
        public String formatOnCloseBraceCharacters = null;

        /**
         * The scope that should be formatted when a close brace is typed. Allowed values are
         * {@link ClientConfigurationFormatScope#CODE_BLOCK CODE_BLOCK} and
         * {@link ClientConfigurationFormatScope#FILE FILE}. Defaults to
         * {@link ClientConfigurationFormatScope#CODE_BLOCK CODE_BLOCK}.
         */
        public ClientConfigurationFormatScope formatOnCloseBraceScope = ClientConfigurationFormatScope.CODE_BLOCK;

        /**
         * Whether or not to degrade gracefully to the entire file if a more constrained scope cannot be found when
         * formatting on close brace. Defaults to false.
         */
        public boolean formatOnCloseBraceDegradeToFile = false;
    }

    /**
     * Client-side workspace symbol settings.
     */
    public static class ClientConfigurationWorkspaceSymbolSettings {
        /**
         * Whether or not the language server can support the IDE's Go To Class action efficiently. Defaults to false.
         */
        public boolean supportsGotoClass = false;
    }

    /**
     * Whether or not the language grammar is case-sensitive. Defaults to false.
     */
    public boolean caseSensitive = false;

    /**
     * Client-side format settings.
     */
    public @NotNull ClientConfigurationFormatSettings format = new ClientConfigurationFormatSettings();

    /**
     * Client-side workspace symbol settings.
     */
    public @NotNull ClientConfigurationWorkspaceSymbolSettings workspaceSymbol = new ClientConfigurationWorkspaceSymbolSettings();
}