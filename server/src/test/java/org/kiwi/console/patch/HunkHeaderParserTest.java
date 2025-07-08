package org.kiwi.console.patch;

import junit.framework.TestCase;
import org.kiwi.console.generate.MalformedHunkException;

public class HunkHeaderParserTest extends TestCase {

    public void testParse() {
        var parser = new HunkHeaderParser("@@ replace 1:1 @@");
        var hh = parser.parse();
        assertSame(Operation.replace, hh.op());
        assertEquals(1, hh.startLine());
        assertEquals(1, hh.endLine());
    }

    public void testCompact() {
        var parser = new HunkHeaderParser("@@replace1:1@@");
        var hh = parser.parse();
        assertSame(Operation.replace, hh.op());
        assertEquals(1, hh.startLine());
        assertEquals(1, hh.endLine());
    }

    public void testExtraWhitespaces() {
        var parser = new HunkHeaderParser(" \t @@ \t replace \t 1 \t : \t 1 \t @@ \t ");
        var hh = parser.parse();
        assertSame(Operation.replace, hh.op());
        assertEquals(1, hh.startLine());
        assertEquals(1, hh.endLine());
    }

    public void testUnrecognizedOp() {
        try {
            var parser = new HunkHeaderParser(" \t @@ \t change \t 1 \t : \t 1 \t @@ \t ");
            parser.parse();
            fail();
        }
        catch (MalformedHunkException e) {
            assertEquals("Malformed hunk:  	 @@ 	 change 	 1 	 : 	 1 	 @@ 	 , position: 10, reason: Unrecognized operation 'change'", e.getMessage());
        }
    }

    public void testMissingSuffix() {
        try {
            var parser = new HunkHeaderParser(" \t @@ \t insert \t 1 \t : \t 1");
            parser.parse();
            fail();
        }
        catch (MalformedHunkException e) {
            assertEquals("Malformed hunk:  	 @@ 	 insert 	 1 	 : 	 1, position: 27, reason: Expected token: '@@', found: <EOF>", e.getMessage());
        }
    }


    public void testTooLargeNumber() {
        try {
            var parser = new HunkHeaderParser(" \t @@ \t insert \t 1 \t : \t 9223372036854775807");
            parser.parse();
            fail();
        }
        catch (MalformedHunkException e) {
            assertEquals("Malformed hunk:  	 @@ 	 insert 	 1 	 : 	 9223372036854775807, position: 27, reason: Invalid integer: 9223372036854775807", e.getMessage());
        }
    }


}