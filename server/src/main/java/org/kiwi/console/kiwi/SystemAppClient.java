package org.kiwi.console.kiwi;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.kiwi.console.util.Result;

public interface SystemAppClient {

    @RequestLine("POST /app")
    @Headers("Content-Type: application/json")
    Result<Long> save(SystemApp app);

    @RequestLine("DELETE /app/{id}")
    Result<Void> delete(@Param("id") long id);

}
