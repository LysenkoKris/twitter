package com.twitter.sms.contracts;

import java.util.List;

public record SubscriptionResponse(Long subscriptionId, List<Long> authors) {

    public static SubscriptionResponse empty() {
        return new SubscriptionResponse(null, List.of());
    }
}
