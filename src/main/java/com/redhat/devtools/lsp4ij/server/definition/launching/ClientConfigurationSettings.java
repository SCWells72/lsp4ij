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
     * Client-side code completion settings.
     */
    public static class ClientConfigurationCompletionSettings {
        /**
         * Whether or not client-side context-aware completion sorting should be used. Defaults to false.
         */
        public boolean useContextAwareSorting = false;
    }

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
         * The specific close brace characters that should trigger on-type formatting. Defaults to the language's close
         * brace characters.
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
        public boolean formatOnCloseBraceDegradeGracefully = false;

        /**
         * Whether or not to format on statement terminator. Defaults to false.
         */
        public boolean formatOnStatementTerminator = false;

        /**
         * The specific statement terminator characters that should trigger on-type formatting.
         */
        public String formatOnStatementTerminatorCharacters = null;

        /**
         * The scope that should be formatted when a statement terminator is typed. Allowed values are
         * {@link ClientConfigurationFormatScope#STATEMENT STATEMENT},
         * {@link ClientConfigurationFormatScope#CODE_BLOCK CODE_BLOCK}, and
         * {@link ClientConfigurationFormatScope#FILE FILE}. Defaults to
         * {@link ClientConfigurationFormatScope#STATEMENT STATEMENT}.
         */
        public ClientConfigurationFormatScope formatOnStatementTerminatorScope = ClientConfigurationFormatScope.STATEMENT;

        /**
         * Whether or not to degrade gracefully to the code block or entire file if a more constrained scope cannot be
         * found when formatting on statement terminator. Defaults to false.
         */
        public boolean formatOnStatementTerminatorDegradeGracefully = false;
    }

    /**
     * Client-side code workspace symbol settings.
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
     * Client-side code completion settings
     */
    public @NotNull ClientConfigurationCompletionSettings completion = new ClientConfigurationCompletionSettings();

    /**
     * Client-side format settings.
     */
    public @NotNull ClientConfigurationFormatSettings format = new ClientConfigurationFormatSettings();

    /**
     * Client-side code workspace symbol settings
     */
    public @NotNull ClientConfigurationWorkspaceSymbolSettings workspaceSymbol = new ClientConfigurationWorkspaceSymbolSettings();
}