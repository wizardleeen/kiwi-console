package org.kiwi.console.patch;

import junit.framework.TestCase;

public class PatchApplyTest extends TestCase {

    public void test() {
        var text = """
                class Product(
                    var name: string
                )
                """;
        var diff = """
                @@ insert 1:1 @@
                enum ProductStatus {
                    ACTIVE,
                    INACTIVE
                    ;
                }
                
                @@ replace 2:2 @@
                    var name: string,
                    var status: ProductStatus
                """;
        var modified = PatchApply.apply(text, diff);
        assertEquals("""
                enum ProductStatus {
                    ACTIVE,
                    INACTIVE
                    ;
                }
                
                class Product(
                    var name: string,
                    var status: ProductStatus
                )
                """, modified);
    }

}