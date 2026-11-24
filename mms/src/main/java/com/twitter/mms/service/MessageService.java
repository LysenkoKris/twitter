package com.twitter.mms.service;

import com.twitter.mms.contracts.FeedItem;
import com.twitter.mms.contracts.FeedResponse;
import com.twitter.mms.contracts.MessageResponse;
import com.twitter.mms.contracts.MessageView;
import com.twitter.mms.domain.Caller;
import com.twitter.mms.domain.MessageText;
import com.twitter.mms.domain.Page;
import com.twitter.mms.domain.RemoteSubscription;
import com.twitter.mms.exception.RequestFailure;
import com.twitter.mms.repository.MessageRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final UmsClient users;
    private final SubscribersClient subscribers;
    private final MessageRepository repository;
    private final FeedCache cache;

    public MessageService(
            UmsClient users,
            SubscribersClient subscribers,
            MessageRepository repository,
            FeedCache cache
    ) {
        this.users = users;
        this.subscribers = subscribers;
        this.repository = repository;
        this.cache = cache;
    }

    public MessageView create(String sessionHeader, String text) {
        Caller caller = requireProducer(sessionHeader);
        MessageView message = repository.insert(caller.userId(), MessageText.parse(text));
        evict(caller.userId());
        return message;
    }

    public MessageView update(String sessionHeader, long id, String text) {
        Caller caller = users.caller(UmsClient.sessionId(sessionHeader));
        requireAuthor(id, caller.userId());
        MessageView updated = repository.update(id, caller.userId(), MessageText.parse(text))
                .orElseThrow(() -> RequestFailure.business(404, "message not found"));
        evict(caller.userId());
        return updated;
    }

    public MessageResponse delete(String sessionHeader, long id) {
        Caller caller = users.caller(UmsClient.sessionId(sessionHeader));
        requireAuthor(id, caller.userId());
        if (!repository.delete(id, caller.userId())) {
            throw RequestFailure.business(404, "message not found");
        }
        evict(caller.userId());
        return new MessageResponse("deleted");
    }

    public List<MessageView> byAuthor(long authorId, Integer limit, Integer offset) {
        return repository.byAuthor(authorId, Page.of(limit, offset));
    }

    public FeedResponse feed(String sessionHeader, Integer limit, Integer offset) {
        UmsClient.sessionId(sessionHeader);
        RemoteSubscription subscription = subscribers.my(sessionHeader);
        if (subscription.isEmpty()) {
            return FeedResponse.empty();
        }
        Page page = Page.of(limit, offset);
        if (page.isDefault()) {
            List<FeedItem> cached = cache.find(subscription.subscriptionId()).orElse(null);
            if (cached != null) {
                return new FeedResponse(subscription.subscriptionId(), cached);
            }
        }
        List<FeedItem> messages = repository.feed(subscription.authors(), page);
        if (page.isDefault()) {
            cache.save(subscription.subscriptionId(), messages);
        }
        return new FeedResponse(subscription.subscriptionId(), messages);
    }

    private Caller requireProducer(String sessionHeader) {
        Caller caller = users.caller(UmsClient.sessionId(sessionHeader));
        if (!caller.has("PRODUCER")) {
            throw RequestFailure.business(403, "producer role is required");
        }
        return caller;
    }

    private void requireAuthor(long messageId, long userId) {
        MessageView message = repository.find(messageId)
                .orElseThrow(() -> RequestFailure.business(404, "message not found"));
        if (message.authorId() != userId) {
            throw RequestFailure.business(403, "only the author can change the message");
        }
    }

    private void evict(long authorId) {
        try {
            cache.evict(subscribers.setsContaining(authorId));
        } catch (RuntimeException exception) {
            log.warn("Feed cache reset because subscription sets are unavailable", exception);
            cache.clear();
        }
    }
}
