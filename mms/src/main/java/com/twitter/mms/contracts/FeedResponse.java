package com.twitter.mms.contracts;

import java.util.List;

public record FeedResponse(Long subscriptionId, List<FeedItem> messages) {

    public static FeedResponse empty() {
        return new FeedResponse(null, List.of());
    }
}
