package org.kiwi.console.kiwi;

import org.kiwi.console.auth.LoginRequest;
import org.kiwi.console.auth.LogoutRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MockUserClient implements UserClient {

    private final MockGenerationConfigClient genConfigClient;

    private final Map<String, User> userMap = new HashMap<>();

    public MockUserClient(MockGenerationConfigClient genConfigClient) {
        this.genConfigClient = genConfigClient;
    }

    @Override
    public User get(String id) {
        return Objects.requireNonNull(userMap.get(id), "User not found: " + id);
    }

    @Override
    public String authenticate(AuthenticateRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void logout(LogoutRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String register(RegisterRequest request) {
        var user = new User(System.currentTimeMillis() + "", request.userName(), request.sysUserId(), List.of(), genConfigClient.getPresetId());
        userMap.put(user.getId(), user);
        return user.getId();
    }

    @Override
    public String getByKiwiUserId(GetByKiwiUserIdRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String generateSsoCode(UserIdRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String loginWithSsoCode(LoginWithSsoCodeRequest request) {
        throw new UnsupportedOperationException();
    }

}
