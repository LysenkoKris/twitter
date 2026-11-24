package com.twitter.sms.contracts;

import java.time.Instant;

public record FollowerResponse(long subscriberId, Instant createdAt) {
}
