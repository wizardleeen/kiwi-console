package org.kiwi.console.generate.qwen;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ImageUrlContent(@JsonProperty("image_url") ImageUrl imageUrl)  implements Content {
    @Override
    public String getType() {
        return "image_url";
    }
}

record ImageUrl(String url) {}
