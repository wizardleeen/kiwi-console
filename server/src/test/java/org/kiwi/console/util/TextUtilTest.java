package org.kiwi.console.util;

import junit.framework.TestCase;

public class TextUtilTest extends TestCase {

    public void testIndent() {
        assertEquals(
                """
                            class Foo {
                                var name = "foo"
                            }
                        """,
                TextUtil.indent("""
                        class Foo {
                            var name = "foo"
                        }
                        """)
        );
    }

}