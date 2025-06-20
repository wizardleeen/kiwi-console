package org.kiwi.console.kiwi;

import feign.Headers;
import feign.RequestLine;
import org.kiwi.console.auth.LoginRequest;
import org.kiwi.console.auth.LogoutRequest;

public interface UserClient {

    @RequestLine("POST /api/user-service/authenticate")
    @Headers("Content-Type: application/json")
    String authenticate(AuthenticateRequest request);

    @RequestLine("POST /api/user-service/login")
    @Headers("Content-Type: application/json")
    String login(LoginRequest request);

    @RequestLine("POST /api/user-service/logout")
    @Headers("Content-Type: application/json")
    Void logout(LogoutRequest request);

    @RequestLine("POST /api/user-service/register")
    @Headers("Content-Type: application/json")
    Void register(RegisterRequest request);
}
