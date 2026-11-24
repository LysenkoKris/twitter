package com.twitter.mms.contracts;

import java.time.Instant;

public record MessageView(long id, String text, long authorId, Instant createdAt, Instant updatedAt) {
}
