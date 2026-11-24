package com.twitter.sms.web;

import com.twitter.sms.contracts.FollowerResponse;
import com.twitter.sms.contracts.MessageResponse;
import com.twitter.sms.contracts.SubscribeRequest;
import com.twitter.sms.contracts.SubscriptionResponse;
import com.twitter.sms.service.SubscriptionService;
import java.net.URI;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscribers")
public class SubscriberController {

    public static final String SESSION_HEADER = "X-Session-Id";

    private final SubscriptionService service;
    private final RequestValidator validator;

    public SubscriberController(SubscriptionService service, RequestValidator validator) {
        this.service = service;
        this.validator = validator;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SubscriptionResponse> subscribe(
            @RequestHeader(value = SESSION_HEADER, required = false) String sessionId,
            @RequestBody SubscribeRequest request
    ) {
        SubscriptionResponse response = service.subscribe(sessionId, validator.authorId(request));
        return ResponseEntity.created(URI.create("/api/v1/subscribers/my")).body(response);
    }

    @DeleteMapping("/{authorId}")
    public MessageResponse unsubscribe(
            @RequestHeader(value = SESSION_HEADER, required = false) String sessionId,
            @PathVariable long authorId
    ) {
        return service.unsubscribe(sessionId, validator.pathId(authorId));
    }

    @GetMapping("/my")
    public SubscriptionResponse my(@RequestHeader(value = SESSION_HEADER, required = false) String sessionId) {
        return service.my(sessionId);
    }

    @GetMapping("/{authorId}/followers")
    public List<FollowerResponse> followers(
            @PathVariable long authorId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        return service.followers(validator.pathId(authorId), limit, offset);
    }

    @GetMapping("/sets")
    public List<Long> sets(@RequestParam(name = "author_id") long authorId) {
        return service.setsContaining(validator.pathId(authorId));
    }
}
