package com.twitter.sms.domain;

import com.twitter.sms.exception.RequestFailure;

public record Page(int limit, int offset) {

    public static Page of(Integer limit, Integer offset) {
        int size = limit == null ? 20 : limit;
        int from = offset == null ? 0 : offset;
        if (size < 1 || size > 100 || from < 0) {
            throw RequestFailure.business(400, "invalid page");
        }
        return new Page(size, from);
    }
}
