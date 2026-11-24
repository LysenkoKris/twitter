package com.twitter.mms.web;

import com.twitter.mms.contracts.CreateMessageRequest;
import com.twitter.mms.contracts.UpdateMessageRequest;
import com.twitter.mms.exception.RequestFailure;
import org.springframework.stereotype.Component;

@Component
public class RequestValidator {

    public String text(CreateMessageRequest request) {
        if (request == null) {
            throw RequestFailure.business(400, "text is required");
        }
        return request.text();
    }

    public String text(UpdateMessageRequest request) {
        if (request == null) {
            throw RequestFailure.business(400, "text is required");
        }
        return request.text();
    }

    public long pathId(long id) {
        if (id <= 0) {
            throw RequestFailure.business(400, "некорректный идентификатор");
        }
        return id;
    }
}
