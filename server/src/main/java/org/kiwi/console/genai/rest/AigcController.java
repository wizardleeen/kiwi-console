package org.kiwi.console.genai.rest;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.kiwi.console.genai.AigcService;
import org.kiwi.console.genai.rest.dto.GenerateRequest;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.ErrorCode;
import org.kiwi.console.util.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/aigc")
public class AigcController {

    private final AigcService aigcService;

    public AigcController(AigcService aigcService) {
        this.aigcService = aigcService;
    }

    @PostMapping("/generate")
    public Result<Void> generate(HttpServletRequest servletRequest, @RequestBody GenerateRequest request) {
        if (request == null || request.appId() <= 0 || request.prompt() == null)
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        aigcService.generate(request, getToken(servletRequest));
        return Result.voidSuccess();
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
