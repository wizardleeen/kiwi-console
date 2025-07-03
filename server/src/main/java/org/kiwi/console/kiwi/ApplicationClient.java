package org.kiwi.console.kiwi;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.kiwi.console.util.SearchResult;

public interface ApplicationClient {

    @RequestLine("POST /api/application/_search")
    @Headers("Content-Type: application/json")
    SearchResult<Application> search(ApplicationSearchRequest request);

    @RequestLine("GET /api/application/{id}")
    Application get(@Param("id") String id);

    @RequestLine("POST /api/application")
    @Headers("Content-Type: application/json")
    String save(Application application);

    @RequestLine("POST /api/application/update-name")
    @Headers("Content-Type: application/json")
    void updateName(UpdateNameRequest request);

    @RequestLine("POST /api/application-service/delete-app")
    @Headers("Content-Type: application/json")
    void delete(DeleteAppRequest request);

}
