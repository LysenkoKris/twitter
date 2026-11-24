package com.twitter.mms.domain;

import com.twitter.mms.exception.RequestFailure;

public final class MessageText {

    private MessageText() {
    }

    public static String parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw RequestFailure.business(400, "text is required");
        }
        String text = raw.trim();
        if (text.length() > 4000) {
            throw RequestFailure.business(400, "text is too long");
        }
        return text;
    }
}
