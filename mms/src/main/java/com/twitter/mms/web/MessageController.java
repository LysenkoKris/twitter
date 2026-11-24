package com.twitter.mms.web;

import com.twitter.mms.contracts.CreateMessageRequest;
import com.twitter.mms.contracts.FeedResponse;
import com.twitter.mms.contracts.MessageResponse;
import com.twitter.mms.contracts.MessageView;
import com.twitter.mms.contracts.UpdateMessageRequest;
import com.twitter.mms.service.MessageService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    public static final String SESSION_HEADER = "X-Session-Id";

    private final MessageService service;
    private final RequestValidator validator;

    public MessageController(MessageService service, RequestValidator validator) {
        this.service = service;
        this.validator = validator;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageView> create(
            @RequestHeader(value = SESSION_HEADER, required = false) String sessionId,
            @RequestBody CreateMessageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(sessionId, validator.text(request)));
    }

    @PutMapping(value = "/{messageId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public MessageView update(
            @RequestHeader(value = SESSION_HEADER, required = false) String sessionId,
            @PathVariable long messageId,
            @RequestBody UpdateMessageRequest request
    ) {
        return service.update(sessionId, validator.pathId(messageId), validator.text(request));
    }

    @DeleteMapping("/{messageId}")
    public MessageResponse delete(
            @RequestHeader(value = SESSION_HEADER, required = false) String sessionId,
            @PathVariable long messageId
    ) {
        return service.delete(sessionId, validator.pathId(messageId));
    }

    @GetMapping
    public List<MessageView> byAuthor(
            @RequestParam(name = "author_id") long authorId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        return service.byAuthor(validator.pathId(authorId), limit, offset);
    }

    @GetMapping("/feed")
    public FeedResponse feed(
            @RequestHeader(value = SESSION_HEADER, required = false) String sessionId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        return service.feed(sessionId, limit, offset);
    }
}
