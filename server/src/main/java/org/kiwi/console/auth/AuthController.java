package org.kiwi.console.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.kiwi.console.kiwi.RegisterRequest;
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

    public AuthController(UserClient userClient) {
        this.userClient = userClient;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResult> login(@RequestBody LoginRequest request) {
        var token = userClient.login(request);
        if (token == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(new LoginResult(token));
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        var auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer "))
            throw new BusinessException(ErrorCode.AUTHENTICATION_ERROR, "Invalid token");
        var token = auth.substring(7);
        userClient.logout(new LogoutRequest(token));
    }

    @PostMapping("/register")
    public void register(@RequestBody RegisterRequest request) {
        userClient.register(request);
    }

}
