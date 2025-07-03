package org.kiwi.console.generate;

import junit.framework.TestCase;

public class CodeSanitizerTest extends TestCase {

    public void test() {
        assertEquals("class Foo(var name)\n",
                CodeSanitizer.sanitizeCode("""
                        ```kiwi
                        class Foo(var name)
                        ```
                        """));

        assertEquals("package foo\n",
                CodeSanitizer.sanitizeCode("""
                        package foo
                        """));

        assertEquals("class Foo {}\n",
                CodeSanitizer.sanitizeCode("""
                        class Foo {}
                        """));

        assertEquals("interface Foo {}\n",
                CodeSanitizer.sanitizeCode("""
                        interface Foo {}
                        """));

        assertEquals("enum Option {}\n",
                CodeSanitizer.sanitizeCode("""
                        enum Option {}
                        """));

        assertEquals("""
                        // Shopping
                        class Product(var name)
                        """,
                CodeSanitizer.sanitizeCode("""
                        // Shopping
                        class Product(var name)
                        """));

        assertEquals(
                "export App = () => <div></div>\n",
                CodeSanitizer.sanitizeCode("""
                        ```tsx
                        export App = () => <div></div>
                        ```
                        """));

        assertEquals(
                "export App = () => <div></div>\n",
                CodeSanitizer.sanitizeCode("""
                        
                        ```tsx
                        export App = () => <div></div>
                        ```
                        """));


        assertEquals(
                "export App = () => <div></div>\n",
                CodeSanitizer.sanitizeCode("""
                        Here is the App.tsx:
                        ```tsx
                        export App = () => <div></div>
                        ```
                        """));

        assertEquals(
                "import { Product } from './api\n",
                CodeSanitizer.sanitizeCode("""
                        import { Product } from './api
                        """));

        assertEquals(
                "class Foo(var name)",
                CodeSanitizer.sanitizeCode("class Foo(var name)")
        );

        assertEquals(
                """
                        {
                            "patch": []
                        }
                        """,
                CodeSanitizer.sanitizeCode("""
                        {
                            "patch": []
                        }
                        """)
        );


        assertEquals(
                """
                        {
                            "patch": []
                        }
                        """,
                CodeSanitizer.sanitizeCode("""
                        Here is the change
                        
                        {
                            "patch": []
                        }
                        """)
        );

        assertEquals(
                "class",
                CodeSanitizer.sanitizeCode("class")
        );

    }
}