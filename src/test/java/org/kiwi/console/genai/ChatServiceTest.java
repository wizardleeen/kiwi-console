package org.kiwi.console.genai;

import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatServiceTest extends TestCase {

    public void testRemoveMarkdownTags() {
        var code = ChatService.removeMarkdownTags("""
                ```kiwi
                class Foo()
                ```
                """);
        log.debug("{}", code);
        assertEquals("""
                
                class Foo()
                """, code);
    }

}