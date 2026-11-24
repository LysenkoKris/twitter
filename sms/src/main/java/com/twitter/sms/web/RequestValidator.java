package com.twitter.sms.web;

import com.twitter.sms.contracts.SubscribeRequest;
import com.twitter.sms.exception.RequestFailure;
import org.springframework.stereotype.Component;

@Component
public class RequestValidator {

    public long authorId(SubscribeRequest request) {
        if (request == null || request.authorId() == null) {
            throw RequestFailure.business(400, "authorId is required");
        }
        return pathId(request.authorId());
    }

    public long pathId(long id) {
        if (id <= 0) {
            throw RequestFailure.business(400, "некорректный идентификатор");
        }
        return id;
    }
}
