package org.kiwi.console.generate;

import junit.framework.TestCase;

public class FormatTest extends TestCase {

    public void testFormat() {
        assertEquals(
                "Hello Kiwi",
                Format.format("Hello {}", "Kiwi")
        );

        assertEquals(
                "Hello Kiwi {}",
                Format.format("Hello {} {}", "Kiwi")
        );
    }

    public void testFormatKeyed() {
        assertEquals(
                "class Foo {}",
                Format.formatKeyed("{example}", "example", "class Foo {}")
        );
    }

}