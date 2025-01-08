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

import com.redhat.devtools.lsp4ij.client.features.LSPFormattingFeature.FormattingScope;
import com.redhat.devtools.lsp4ij.fixtures.LSPClientSideOnTypeFormattingFixtureTestCase;

/**
 * TypeScript-based client-side on-type formatting tests.
 */
public class TypeScriptClientSideOnTypeFormattingTest extends LSPClientSideOnTypeFormattingFixtureTestCase {

    private static final String TEST_FILE_NAME = "test.ts";

    public TypeScriptClientSideOnTypeFormattingTest() {
        super("*.ts");
    }

    // SIMPLE FORMAT-ON-CLOSE-BRACE TESTS

    // language=json
    private static final String SIMPLE_FOCB_MOCK_SELECTION_RANGE_JSON = """
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
    private static final String SIMPLE_FOCB_MOCK_FOLDING_RANGE_JSON = """
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
    private static final String SIMPLE_FOCB_MOCK_RANGE_FORMATTING_JSON = """
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
    private static final String SIMPLE_FOCB_FILE_BODY_BEFORE = """
            export class Foo {
                bar() {
            console.log('Hello, world.');
                // type }
            }
            """;

    public void testSimpleFormatOnCloseBraceDefaults() {
        assertOnTypeFormatting(
                TEST_FILE_NAME,
                SIMPLE_FOCB_FILE_BODY_BEFORE,
                // language=typescript
                """
                        export class Foo {
                            bar() {
                        console.log('Hello, world.');
                            }
                        }
                        """,
                SIMPLE_FOCB_MOCK_SELECTION_RANGE_JSON,
                SIMPLE_FOCB_MOCK_FOLDING_RANGE_JSON,
                SIMPLE_FOCB_MOCK_RANGE_FORMATTING_JSON,
                null // No-op as the default is disabled
        );
    }

    public void testSimpleFormatOnCloseBraceEnabled() {
        assertOnTypeFormatting(
                TEST_FILE_NAME,
                SIMPLE_FOCB_FILE_BODY_BEFORE,
                // language=typescript
                """
                        export class Foo {
                            bar() {
                                console.log('Hello, world.');
                            }
                        }
                        """,
                SIMPLE_FOCB_MOCK_SELECTION_RANGE_JSON,
                SIMPLE_FOCB_MOCK_FOLDING_RANGE_JSON,
                SIMPLE_FOCB_MOCK_RANGE_FORMATTING_JSON,
                clientConfiguration -> clientConfiguration.format.formatOnCloseBrace = true
        );
    }

    public void testSimpleFormatOnCloseBraceEnabledNoCurlyBrace() {
        assertOnTypeFormatting(
                TEST_FILE_NAME,
                SIMPLE_FOCB_FILE_BODY_BEFORE,
                // language=typescript
                """
                        export class Foo {
                            bar() {
                        console.log('Hello, world.');
                            }
                        }
                        """,
                SIMPLE_FOCB_MOCK_SELECTION_RANGE_JSON,
                SIMPLE_FOCB_MOCK_FOLDING_RANGE_JSON,
                SIMPLE_FOCB_MOCK_RANGE_FORMATTING_JSON,
                clientConfiguration -> {
                    clientConfiguration.format.formatOnCloseBrace = true;
                    // Explicitly specify close brace characters that don't include right curly brace
                    clientConfiguration.format.formatOnCloseBraceCharacters = "])";
                }
        );
    }

    // COMPLEX FORMAT-ON-CLOSE-BRACE TESTS

    // language=json
    private static final String COMPLEX_FOCB_MOCK_SELECTION_RANGE_JSON = """
            [
              {
                "range": {
                  "start": {
                    "line": 4,
                    "character": 0
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
            ]
            """;

    // language=json
    private static final String COMPLEX_FOCB_MOCK_FOLDING_RANGE_JSON = """
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
    private static final String COMPLEX_FOCB_MOCK_RANGE_FORMATTING_JSON = """
            [
              {
                "range": {
                  "start": {
                    "line": 1,
                    "character": 0
                  },
                  "end": {
                    "line": 1,
                    "character": 0
                  }
                },
                "newText": "    "
              },
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
              },
              {
                "range": {
                  "start": {
                    "line": 3,
                    "character": 0
                  },
                  "end": {
                    "line": 3,
                    "character": 0
                  }
                },
                "newText": "    "
              }
            ]
            """;

    // No language injection here because there are syntax errors
    private static final String COMPLEX_FOCB_FILE_BODY_BEFORE = """
            export class Foo {
            bar() {
            console.log('Hello, world.');
            }
            // type }
            """;

    public void testComplexFormatOnCloseBraceDefaults() {
        assertOnTypeFormatting(
                TEST_FILE_NAME,
                COMPLEX_FOCB_FILE_BODY_BEFORE,
                // language=typescript
                """
                        export class Foo {
                        bar() {
                        console.log('Hello, world.');
                        }
                        }
                        """,
                COMPLEX_FOCB_MOCK_SELECTION_RANGE_JSON,
                COMPLEX_FOCB_MOCK_FOLDING_RANGE_JSON,
                COMPLEX_FOCB_MOCK_RANGE_FORMATTING_JSON,
                null // No-op as the default is disabled
        );
    }

    public void testComplexFormatOnCloseBraceEnabled() {
        assertOnTypeFormatting(
                TEST_FILE_NAME,
                COMPLEX_FOCB_FILE_BODY_BEFORE,
                // language=typescript
                """
                        export class Foo {
                            bar() {
                                console.log('Hello, world.');
                            }
                        }
                        """,
                COMPLEX_FOCB_MOCK_SELECTION_RANGE_JSON,
                COMPLEX_FOCB_MOCK_FOLDING_RANGE_JSON,
                COMPLEX_FOCB_MOCK_RANGE_FORMATTING_JSON,
                clientConfiguration -> clientConfiguration.format.formatOnCloseBrace = true
        );
    }

    public void testComplexFormatOnCloseBraceEnabledNoCurlyBrace() {
        assertOnTypeFormatting(
                TEST_FILE_NAME,
                COMPLEX_FOCB_FILE_BODY_BEFORE,
                // language=typescript
                """
                        export class Foo {
                        bar() {
                        console.log('Hello, world.');
                        }
                        }
                        """,
                COMPLEX_FOCB_MOCK_SELECTION_RANGE_JSON,
                COMPLEX_FOCB_MOCK_FOLDING_RANGE_JSON,
                COMPLEX_FOCB_MOCK_RANGE_FORMATTING_JSON,
                clientConfiguration -> {
                    clientConfiguration.format.formatOnCloseBrace = true;
                    // Explicitly specify close brace characters that don't include right curly brace
                    clientConfiguration.format.formatOnCloseBraceCharacters = "])";
                }
        );
    }

    // FORMAT-ON-CLOSE-BRACE SCOPE TESTS

    public void testFormatOnCloseBraceEnabledFileScope() {
        // language=json
        String mockRangeFormattingJson = """
                [
                  {
                    "range": {
                      "start": {
                        "line": 1,
                        "character": 0
                      },
                      "end": {
                        "line": 1,
                        "character": 0
                      }
                    },
                    "newText": "    "
                  },
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
                  },
                  {
                    "range": {
                      "start": {
                        "line": 3,
                        "character": 0
                      },
                      "end": {
                        "line": 3,
                        "character": 0
                      }
                    },
                    "newText": "            "
                  },
                  {
                    "range": {
                      "start": {
                        "line": 4,
                        "character": 0
                      },
                      "end": {
                        "line": 4,
                        "character": 0
                      }
                    },
                    "newText": "        "
                  },
                  {
                    "range": {
                      "start": {
                        "line": 5,
                        "character": 0
                      },
                      "end": {
                        "line": 5,
                        "character": 0
                      }
                    },
                    "newText": "    "
                  }
                ]
                """;

        // No language injection here because there are syntax errors
        String fileBodyBefore = """
                export class Foo {
                bar() {
                if (true) {
                console.log('Hello, world.');
                // type }
                }
                }
                """;

        assertOnTypeFormatting(
                TEST_FILE_NAME,
                fileBodyBefore,
                // language=typescript
                """
                        export class Foo {
                            bar() {
                                if (true) {
                                    console.log('Hello, world.');
                                }
                            }
                        }
                        """,
                "[]",
                "[]",
                mockRangeFormattingJson,
                clientConfiguration -> {
                    clientConfiguration.format.formatOnCloseBrace = true;
                    clientConfiguration.format.formatOnCloseBraceScope = FormattingScope.FILE;
                }
        );
    }
}
