package org.kiwi.console.util;

import feign.Response;
import feign.codec.ErrorDecoder;

import java.io.IOException;
import java.io.InputStream;

public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        try (InputStream bodyIs = response.body().asInputStream()) {
            var errorResponse = Utils.readJsonBytes(bodyIs, ErrorResponse.class);
            throw new BusinessException(ErrorCode.REQUEST_ERROR, errorResponse.message());
        } catch (IOException e) {
            return defaultDecoder.decode(methodKey, response);
        }
    }
}