package com.twitter.mms.domain;

import java.util.List;

public record RemoteSubscription(Long subscriptionId, List<Long> authors) {

    public boolean isEmpty() {
        return subscriptionId == null || authors.isEmpty();
    }
}
