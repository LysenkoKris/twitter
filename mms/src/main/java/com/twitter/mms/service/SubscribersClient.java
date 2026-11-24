package com.twitter.mms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.mms.domain.RemoteSubscription;
import com.twitter.mms.exception.RequestFailure;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpRequest;
import org.springframework.web.client.RestClient;

public class SubscribersClient {

    private final RestClient client;
    private final ObjectMapper objectMapper;

    public SubscribersClient(RestClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public RemoteSubscription my(String sessionHeader) {
        long sessionId = UmsClient.sessionId(sessionHeader);
        JsonNode body = exchange(client.get().uri("/api/v1/subscribers/my").header("X-Session-Id", Long.toString(sessionId)));
        List<Long> authors = new ArrayList<>();
        body.path("authors").forEach(author -> authors.add(author.asLong()));
        Long subscriptionId = body.path("subscriptionId").isNumber() ? body.get("subscriptionId").asLong() : null;
        return new RemoteSubscription(subscriptionId, List.copyOf(authors));
    }

    public List<Long> setsContaining(long authorId) {
        JsonNode body = exchange(client.get().uri("/api/v1/subscribers/sets?author_id={authorId}", authorId));
        if (!body.isArray()) {
            throw RequestFailure.transport(502, "subscribers service unavailable");
        }
        List<Long> ids = new ArrayList<>();
        body.forEach(id -> ids.add(id.asLong()));
        return List.copyOf(ids);
    }

    private JsonNode exchange(RestClient.RequestHeadersSpec<?> request) {
        try {
            return request.exchange(this::read);
        } catch (RequestFailure failure) {
            throw failure;
        } catch (RuntimeException exception) {
            RequestFailure failure = unwrap(exception);
            if (failure != null) {
                throw failure;
            }
            throw RequestFailure.transport(502, "subscribers service unavailable");
        }
    }

    private JsonNode read(HttpRequest request, RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response)
            throws IOException {
        return UmsClient.readBody(objectMapper, response, "subscribers service unavailable");
    }

    private static RequestFailure unwrap(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof RequestFailure failure) {
                return failure;
            }
            current = current.getCause();
        }
        return null;
    }
}
