package org.kiwi.console.kiwi;

import feign.Headers;
import feign.RequestLine;
import org.kiwi.console.auth.IssueTokenRequest;

public interface KiwiUserClient {

    @RequestLine("POST /internal-api/user/issue-token")
    @Headers("Content-Type: application/json")
    String issueToken(IssueTokenRequest request);

    @RequestLine("POST /internal-api/user/authenticate")
    @Headers("Content-Type: application/json")
    String authenticate(SysAuthenticateRequest request);

    @RequestLine("POST /internal-api/user/save")
    @Headers("Content-Type: application/json")
    String save(SystemUser user);

    @RequestLine("POST /internal-api/user/logout")
    @Headers("Content-Type: application/json")
    void logout(SysLogoutRequest request);

}
