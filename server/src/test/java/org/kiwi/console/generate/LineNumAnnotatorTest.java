package org.kiwi.console.generate;

import junit.framework.TestCase;

public class LineNumAnnotatorTest extends TestCase {

    public void test() {
        assertEquals(
                """
                        1   | class Product(
                        2   |     var name: string,
                        3   |     var price: double
                        4   |     var stock: int
                        5   | )
                        6   |\s""",
                LineNumAnnotator.annotate(
                        """
                            class Product(
                                var name: string,
                                var price: double
                                var stock: int
                            )
                            """
                )
        );
    }

}