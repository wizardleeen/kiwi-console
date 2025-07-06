package org.kiwi.console.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.kiwi.console.kiwi.RegisterRequest;
import org.kiwi.console.kiwi.SysLogoutRequest;
import org.kiwi.console.kiwi.SysUserClient;
import org.kiwi.console.kiwi.UserClient;
import org.kiwi.console.util.BusinessException;
import org.kiwi.console.util.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<LoginResult> login(@RequestBody LoginRequest request) {
        var userId = userClient.login(request).userId();
        if (userId == null)
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED);
        var user = userClient.get(userId);
        var token = sysUserClient.issueToken(new IssueTokenRequest(user.getSysUserId()));
        return ResponseEntity.ok(new LoginResult(token));
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

}
