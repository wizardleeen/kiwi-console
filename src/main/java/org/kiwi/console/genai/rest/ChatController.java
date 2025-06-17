package org.kiwi.console.genai.rest;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.kiwi.console.genai.ChatService;
import org.kiwi.console.genai.rest.dto.ChatRequest;
import org.kiwi.console.genai.rest.dto.ChatResponse;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.ErrorCode;
import org.kiwi.console.util.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public Result<ChatResponse> chat(HttpServletRequest servletRequest,  @RequestBody ChatRequest request) {
        return Result.success(chatService.chat(request, getToken(servletRequest)));
    }

    private String getToken(HttpServletRequest servletRequest) {
        var cookies = servletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("__token_2__"))
                    return cookie.getValue();
            }
        }
        throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
    }

}
