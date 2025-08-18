package org.kiwi.console.kiwi;

import org.kiwi.console.auth.LoginRequest;
import org.kiwi.console.auth.LogoutRequest;
import org.kiwi.console.util.Constants;
import org.kiwi.console.util.Utils;

import java.util.List;
import java.util.Random;

public class UserService implements UserClient {
    private final KiwiUserClient kiwiUserClient;
    private final UserClient userClient;

    public UserService(String url, long appId) {
        this.kiwiUserClient = Utils.createFeignClient(url, KiwiUserClient.class);
        this.userClient = Utils.createKiwiFeignClient(url, UserClient.class, appId);
    }

    @Override
    public User get(String id) {
        return userClient.get(id);
    }

    @Override
    public String authenticate(AuthenticateRequest request) {
        return userClient.authenticate(request);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        return userClient.login(request);
    }

    @Override
    public void logout(LogoutRequest request) {
        userClient.logout(request);
    }

    @Override
    public String register(RegisterRequest request) {
        var kiwiUserId = kiwiUserClient.save(new SystemUser(request.userName(), request.userName(),
                request.password(),
                List.of()
        ));
        return userClient.register(new RegisterRequest(request.userName(), request.password(), kiwiUserId));
    }

    @Override
    public String getByKiwiUserId(GetByKiwiUserIdRequest request) {
        return userClient.getByKiwiUserId(request);
    }

    @Override
    public String generateSsoCode(UserIdRequest request) {
        return userClient.generateSsoCode(request);
    }

    @Override
    public String loginWithSsoCode(LoginWithSsoCodeRequest request) {
        return userClient.loginWithSsoCode(request);
    }

    public static void main(String[] args) {
        var client = new UserService(
                "http://localhost:8080",
                Constants.CHAT_APP_ID
        );

//        var user = client.get("01e6f0ddb90700");

//        System.out.println("System user ID " + user.getSysUserId());


        var rand = new Random();
        for (int i = 0; i < 10; i++) {
            var userId = client.register(new RegisterRequest(
                    "user" + rand.nextInt(10000),
                    "123456"
            ));
            var user = client.get(userId);
            client.getByKiwiUserId(new GetByKiwiUserIdRequest(user.getKiwiUserId()));
        }
//        var userId = client.register(new RegisterRequest(
//                "hqq", "123456"
//        ));

//        client.sysUserClient.logout(new SysLogoutRequest("1d950468-aee7-4422-9842-668ca1e9cb0c"));

//        var userId = client.login(new LoginRequest("leen", "123456")).userId();
//        var user = client.get(userId);
//
//        System.out.println("System user ID: " + user.getSysUserId());
//
//        var token = client.sysUserClient.issueToken(new IssueTokenRequest(user.getSysUserId()));
//
//        String token = "1d950468-aee7-4422-9842-668ca1e9cb0c";
//
//        System.out.println("Token: " + token);
//
//        var kiwiUserId = client.sysUserClient.authenticate(new SysAuthenticateRequest(token));
//
//        System.out.println("System user ID: " + kiwiUserId);
//
//        var userId2 = client.getBySysUserId(new GetBySysUserIdRequest(kiwiUserId));
//        System.out.println("User ID: " + userId2);
    }

}
