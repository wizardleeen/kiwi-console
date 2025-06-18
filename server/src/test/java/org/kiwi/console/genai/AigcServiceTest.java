package org.kiwi.console.genai;

import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.kiwi.console.genai.rest.dto.GenerateRequest;

@Slf4j
public class AigcServiceTest extends TestCase {


    public void testChat() {
        var compiler = new MockAgentCompiler();
        var chatService = new AigcService(new MockAgent(), compiler);
        chatService.generate(new GenerateRequest(1, "foo"), "tk");
        assertEquals("foo", compiler.getCode(1));

        chatService.generate(new GenerateRequest(1, "changed"), "tk");
        assertEquals("foo-changed", compiler.getCode(1));

        chatService.generate(new GenerateRequest(1, "error"), "tk");
        assertEquals("foo-changed-error-fixed", compiler.getCode(1));
    }

    public void testRemoveMarkdownTags() {
        var code = AigcService.removeMarkdownTags("""
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