package org.kiwi.console.patch;

import junit.framework.TestCase;

public class PatchReaderTest extends TestCase {

    public void testInsert() {
        var patch = new PatchReader("""
                @@ insert 1:1 @@
                import org.metavm.api.Index
                """).read();
        assertEquals(1, patch.hunks().size());
        var hunk = patch.hunks().getFirst();
        assertEquals(Operation.insert, hunk.op());
        assertEquals(1, hunk.startLine());
        assertEquals(1, hunk.endLine());
        assertEquals("import org.metavm.api.Index\n", hunk.content());
    }

    public void testInsertWithEmptyFirstLine() {
        var patch = new PatchReader("""
                @@ insert 1:1 @@
                
                import org.metavm.api.Index
                """).read();
        assertEquals(1, patch.hunks().size());
        var hunk = patch.hunks().getFirst();
        assertEquals(Operation.insert, hunk.op());
        assertEquals(1, hunk.startLine());
        assertEquals(1, hunk.endLine());
        assertEquals("\nimport org.metavm.api.Index\n", hunk.content());
    }


    public void testDelete() {
        var patch = new PatchReader("@@ delete 2:3 @@").read();
        assertEquals(1, patch.hunks().size());
        var hunk = patch.hunks().getFirst();
        assertEquals(Operation.delete, hunk.op());
        assertEquals(2, hunk.startLine());
        assertEquals(3, hunk.endLine());
        assertEquals("", hunk.content());
    }

    public void testReplace() {
        var patch = new PatchReader("""
                @@ replace 5:7 @@
                class Product(
                    var name: string,
                    var price: double,
                    vr stock: int
                )
                """).read();
        assertEquals(1, patch.hunks().size());
        var hunk = patch.hunks().getFirst();
        assertEquals(Operation.replace, hunk.op());
        assertEquals(5, hunk.startLine());
        assertEquals(7, hunk.endLine());
        assertEquals("""
                class Product(
                    var name: string,
                    var price: double,
                    vr stock: int
                )
                """, hunk.content());
    }

    public void testMultiHunks() {
        var patch = new PatchReader("""
                @@ insert 1:1 @@
                import org.metavm.api.Index
                @@ delete 2:3 @@
                @@ replace 5:7 @@
                class Product(
                    var name: string,
                    var price: double,
                    var stock: int
                )
                """).read();
        assertEquals(3, patch.hunks().size());
    }

}