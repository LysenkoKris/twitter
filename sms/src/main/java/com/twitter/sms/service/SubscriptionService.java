package com.twitter.sms.service;

import com.twitter.sms.contracts.FollowerResponse;
import com.twitter.sms.contracts.MessageResponse;
import com.twitter.sms.contracts.SubscriptionResponse;
import com.twitter.sms.domain.Author;
import com.twitter.sms.domain.Caller;
import com.twitter.sms.domain.Page;
import com.twitter.sms.exception.RequestFailure;
import com.twitter.sms.repository.SubscriptionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class SubscriptionService {

    private final UmsClient users;
    private final SubscriptionRepository repository;
    private final TransactionTemplate transactions;

    public SubscriptionService(UmsClient users, SubscriptionRepository repository, TransactionTemplate transactions) {
        this.users = users;
        this.repository = repository;
        this.transactions = transactions;
    }

    public SubscriptionResponse subscribe(String sessionHeader, long authorId) {
        Caller caller = users.caller(UmsClient.sessionId(sessionHeader));
        if (!caller.has("SUBSCRIBER")) {
            throw RequestFailure.business(403, "subscriber role is required");
        }
        if (caller.userId() == authorId) {
            throw RequestFailure.business(400, "cannot subscribe to yourself");
        }
        Author author = users.author(authorId);
        if (!author.has("PRODUCER")) {
            throw RequestFailure.business(400, "author is not a producer");
        }
        return transactions.execute(status -> {
            if (repository.exists(caller.userId(), authorId)) {
                throw RequestFailure.business(409, "already subscribed");
            }
            repository.insert(caller.userId(), authorId);
            repository.refresh(caller.userId());
            return repository.findMy(caller.userId())
                    .orElseThrow(() -> RequestFailure.transport(500, "internal error"));
        });
    }

    public MessageResponse unsubscribe(String sessionHeader, long authorId) {
        Caller caller = users.caller(UmsClient.sessionId(sessionHeader));
        Boolean removed = transactions.execute(status -> {
            boolean deleted = repository.delete(caller.userId(), authorId);
            if (deleted) {
                repository.refresh(caller.userId());
            }
            return deleted;
        });
        if (!Boolean.TRUE.equals(removed)) {
            throw RequestFailure.business(404, "subscription not found");
        }
        return new MessageResponse("unsubscribed");
    }

    public SubscriptionResponse my(String sessionHeader) {
        Caller caller = users.caller(UmsClient.sessionId(sessionHeader));
        return repository.findMy(caller.userId()).orElseGet(SubscriptionResponse::empty);
    }

    public List<FollowerResponse> followers(long authorId, Integer limit, Integer offset) {
        return repository.followers(authorId, Page.of(limit, offset));
    }

    public List<Long> setsContaining(long authorId) {
        return repository.setsContaining(authorId);
    }
}
