package org.kiwi.console.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.kiwi.console.kiwi.*;
import org.kiwi.console.kiwi.RegisterRequest;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.ErrorCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserClient userClient;
    private final SysUserClient sysUserClient;

    public AuthController(UserClient userClient, SysUserClient sysUserClient) {
        this.userClient = userClient;
        this.sysUserClient = sysUserClient;
    }

    @PostMapping("/login")
    public LoginResult login(@RequestBody LoginRequest request) {
        var userId = userClient.login(request).userId();
        if (userId == null)
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED);
        return new LoginResult(issueToken(userId));
    }

    private String issueToken(String userId) {
        var user = userClient.get(userId);
        return sysUserClient.issueToken(new IssueTokenRequest(user.getSysUserId()));
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        var auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer "))
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Invalid token");
        var token = auth.substring(7);
        sysUserClient.logout(new SysLogoutRequest(token));
    }

    @PostMapping("/register")
    public void register(@RequestBody org.kiwi.console.auth.RegisterRequest request) {
        userClient.register(new RegisterRequest(request.userName(), request.password()));
    }

    @PostMapping("/generate-sso-code")
    public SsoCodeResult generateSsoCode(@AuthenticationPrincipal String userId) {
        return new SsoCodeResult(userClient.generateSsoCode(new UserIdRequest(userId)));
    }

    @PostMapping("/login-with-sso-code")
    public LoginResult loginWithSsoCode(@RequestBody LoginWithSsoCodeRequest request) {
        var userId = userClient.loginWithSsoCode(new org.kiwi.console.kiwi.LoginWithSsoCodeRequest(request.code()));
        if (userId != null)
            return new LoginResult(issueToken(userId));
        else
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Invalid SSO code");
    }

}
