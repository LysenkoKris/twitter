package com.twitter.mms.service;

import com.twitter.mms.contracts.FeedItem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class FeedCache {

    private final ConcurrentHashMap<String, List<FeedItem>> feeds = new ConcurrentHashMap<>();

    public Optional<List<FeedItem>> find(long subscriptionId) {
        return Optional.ofNullable(feeds.get(key(subscriptionId)));
    }

    public void save(long subscriptionId, List<FeedItem> messages) {
        feeds.put(key(subscriptionId), List.copyOf(messages));
    }

    public void evict(Collection<Long> subscriptionIds) {
        subscriptionIds.forEach(id -> feeds.remove(key(id)));
    }

    public void clear() {
        feeds.clear();
    }

    static String key(long subscriptionId) {
        return "feed:" + subscriptionId;
    }
}
