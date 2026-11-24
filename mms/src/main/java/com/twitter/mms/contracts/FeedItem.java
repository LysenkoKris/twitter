package com.twitter.mms.contracts;

import java.time.Instant;

public record FeedItem(long id, long authorId, String text, Instant createdAt) {
}
