package org.kiwi.console.kiwi;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface KiwiAppClient {

    @RequestLine("POST /internal-api/app/save")
    @Headers("Content-Type: application/json")
    long save(SystemApp app);

    @RequestLine("POST /internal-api/app/update-name")
    @Headers("Content-Type: application/json")
    void updateName(UpdateAppNameRequest req);

    @RequestLine("DELETE /internal-api/app/delete/{id}")
    void delete(@Param("id") long id);

}
