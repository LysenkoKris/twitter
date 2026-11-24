package com.twitter.mms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.mms.domain.Caller;
import com.twitter.mms.exception.RequestFailure;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpRequest;
import org.springframework.web.client.RestClient;

public class UmsClient {

    private final RestClient client;
    private final ObjectMapper objectMapper;

    public UmsClient(RestClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public Caller caller(long sessionId) {
        JsonNode body = get("/api/sessions/{id}", sessionId);
        return new Caller(body.path("userId").asLong(), texts(body.path("roles")));
    }

    private JsonNode get(String path, Object... variables) {
        try {
            return client.get().uri(path, variables).exchange(this::read);
        } catch (RequestFailure failure) {
            throw failure;
        } catch (RuntimeException exception) {
            RequestFailure failure = unwrap(exception);
            if (failure != null) {
                throw failure;
            }
            throw RequestFailure.transport(502, "users service unavailable");
        }
    }

    private JsonNode read(HttpRequest request, RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response)
            throws IOException {
        return readBody(objectMapper, response, "users service unavailable");
    }

    static JsonNode readBody(
            ObjectMapper objectMapper,
            RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response,
            String unavailable
    ) throws IOException {
        byte[] bytes = response.getBody().readAllBytes();
        if (response.getStatusCode().isError() || bytes.length == 0) {
            throw RequestFailure.transport(502, unavailable);
        }
        JsonNode body = objectMapper.readTree(bytes);
        if (body.hasNonNull("code")) {
            throw RequestFailure.business(body.get("code").asInt(), body.path("message").asText("request rejected"));
        }
        return body;
    }

    static long sessionId(String header) {
        if (header == null || header.isBlank()) {
            throw RequestFailure.business(401, "invalid session");
        }
        try {
            long id = Long.parseLong(header.trim());
            if (id <= 0) {
                throw new NumberFormatException("non positive");
            }
            return id;
        } catch (NumberFormatException exception) {
            throw RequestFailure.business(400, "некорректный идентификатор");
        }
    }

    private static List<String> texts(JsonNode node) {
        List<String> values = new ArrayList<>();
        node.forEach(item -> values.add(item.asText()));
        return List.copyOf(values);
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
