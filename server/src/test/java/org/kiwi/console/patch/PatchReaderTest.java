package org.kiwi.console.patch;

import junit.framework.TestCase;

public class PatchReaderTest extends TestCase {

    public void testInsert() {
        var patch = new PatchReader("""
                @@ src/main.kiwi @@
                import org.metavm.api.Index
                """).read();
        assertEquals(1, patch.addedFiles().size());
        var file = patch.addedFiles().getFirst();
        assertEquals("import org.metavm.api.Index\n", file.content());
    }

    public void testInsertWithEmptyFirstLine() {
        var patch = new PatchReader("""
                @@ src/main.kiwi @@
                
                import org.metavm.api.Index
                """).read();
        assertEquals(1, patch.addedFiles().size());
        var hunk = patch.addedFiles().getFirst();
        assertEquals("\nimport org.metavm.api.Index\n", hunk.content());
    }


    public void testDelete() {
        var patch = new PatchReader("""
                @@ --src/main.kiwi @@
                @@ --src/test.kiwi @@
                """).read();
        assertEquals(0, patch.addedFiles().size());
        assertEquals(2, patch.removedFiles().size());
        assertEquals("src/main.kiwi", patch.removedFiles().getFirst().toString());
        assertEquals("src/test.kiwi", patch.removedFiles().get(1).toString());
    }

    public void testExtraTextInTheBeginning() {
        var patch = new PatchReader("""
                Extra Text
                @@ src/main.kiwi @@
                import org.metavm.api.Index
                """).read();
        assertEquals(1, patch.addedFiles().size());
        var file = patch.addedFiles().getFirst();
        assertEquals("import org.metavm.api.Index\n", file.content());
    }

    public void testMultiHunks() {
        var patch = new PatchReader("""
                @@ src/order.kiwi @@
                import org.metavm.api.Index
                @@ src/product.kiwi @@
                class Product(
                    var name: string,
                    var price: double,
                    var stock: int
                )
                @@ --src/coupon.kiwi @@
                """).read();
        assertEquals(2, patch.addedFiles().size());
        assertEquals(1, patch.removedFiles().size());
    }

}