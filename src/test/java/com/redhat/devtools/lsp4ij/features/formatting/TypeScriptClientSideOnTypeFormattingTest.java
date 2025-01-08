/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package com.redhat.devtools.lsp4ij.features.formatting;

import com.redhat.devtools.lsp4ij.fixtures.LSPClientSideOnTypeFormattingFixtureTestCase;

/**
 * TypeScript-based client-side on-type formatting tests.
 */
public class TypeScriptClientSideOnTypeFormattingTest extends LSPClientSideOnTypeFormattingFixtureTestCase {

    private static final String TEST_FILE_NAME = "test.ts";

    public TypeScriptClientSideOnTypeFormattingTest() {
        super("*.ts");
    }

    public void testFormatOnCloseBrace() {
        // language=json
        String mockSelectionRangeJson = """
                [
                  {
                    "range": {
                      "start": {
                        "line": 1,
                        "character": 10
                      },
                      "end": {
                        "line": 4,
                        "character": 1
                      }
                    },
                    "parent": {
                      "range": {
                        "start": {
                          "line": 1,
                          "character": 4
                        },
                        "end": {
                          "line": 4,
                          "character": 1
                        }
                      },
                      "parent": {
                        "range": {
                          "start": {
                            "line": 0,
                            "character": 0
                          },
                          "end": {
                            "line": 4,
                            "character": 1
                          }
                        }
                      }
                    }
                  }
                ]
                """;
        // language=json
        String mockFoldingRangeJson = """
                [
                  {
                    "startLine": 0,
                    "endLine": 3
                  },
                  {
                    "startLine": 1,
                    "endLine": 2
                  }
                ]
                """;
        // language=json
        String mockRangeFormattingJson = """
                [
                  {
                    "range": {
                      "start": {
                        "line": 2,
                        "character": 0
                      },
                      "end": {
                        "line": 2,
                        "character": 0
                      }
                    },
                    "newText": "        "
                  }
                ]
                """;

        // No language injection here because there are syntax errors
        String fileBodyBefore = """
                export class Foo {
                    bar() {
                console.log('Hello, world.');
                    // type }
                }
                """;

        // First test with the format-on-close-brace disabled
        assertOnTypeFormatting(
                TEST_FILE_NAME,
                // No language injection here because there are syntax errors
                fileBodyBefore,
                // language=typescript
                """
                        export class Foo {
                            bar() {
                        console.log('Hello, world.');
                            }
                        }
                        """,
                mockSelectionRangeJson,
                mockFoldingRangeJson,
                mockRangeFormattingJson,
                clientConfiguration -> clientConfiguration.format.formatOnCloseBrace = false
        );

        // Then test with the format-on-close-brace enabled
        assertOnTypeFormatting(
                TEST_FILE_NAME,
                fileBodyBefore,
                // language=typescript
                """
                        export class Foo {
                            bar() {
                                console.log('Hello, world.');
                            }
                        }
                        """,
                mockSelectionRangeJson,
                mockFoldingRangeJson,
                mockRangeFormattingJson,
                clientConfiguration -> clientConfiguration.format.formatOnCloseBrace = true
        );
    }
}
