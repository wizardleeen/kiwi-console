package org.kiwi.console.kiwi;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.kiwi.console.auth.LoginRequest;
import org.kiwi.console.auth.LogoutRequest;

public interface UserClient {

    @RequestLine("GET /api/user/{id}")
    User get(@Param("id") String id);

    @RequestLine("POST /api/user-service/authenticate")
    @Headers("Content-Type: application/json")
    String authenticate(AuthenticateRequest request);

    @RequestLine("POST /api/user-service/login")
    @Headers("Content-Type: application/json")
    LoginResponse login(LoginRequest request);

    @RequestLine("POST /api/user-service/logout")
    @Headers("Content-Type: application/json")
    void logout(LogoutRequest request);

    @RequestLine("POST /api/user-service/register")
    @Headers("Content-Type: application/json")
    String register(RegisterRequest request);

    @RequestLine("POST /api/user-service/get-by-sys-user-id")
    @Headers("Content-Type: application/json")
    String getBySysUserId(GetBySysUserIdRequest request);

}
