package com.twitter.sms.repository;

import com.twitter.sms.contracts.FollowerResponse;
import com.twitter.sms.contracts.SubscriptionResponse;
import com.twitter.sms.domain.Page;
import com.twitter.sms.exception.RequestFailure;
import java.sql.Array;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SubscriptionRepository {

    private final JdbcTemplate jdbc;

    public SubscriptionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean exists(long subscriberId, long authorId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM subscriptions WHERE subscriber_id = ? AND author_id = ?",
                Integer.class,
                subscriberId,
                authorId
        );
        return count != null && count > 0;
    }

    public void insert(long subscriberId, long authorId) {
        try {
            jdbc.update(
                    "INSERT INTO subscriptions (subscriber_id, author_id) VALUES (?, ?)",
                    subscriberId,
                    authorId
            );
        } catch (DataIntegrityViolationException exception) {
            String state = sqlState(exception);
            if ("23505".equals(state)) {
                throw RequestFailure.business(409, "already subscribed");
            }
            if ("23514".equals(state)) {
                throw RequestFailure.business(400, "cannot subscribe to yourself");
            }
            throw exception;
        }
    }

    public boolean delete(long subscriberId, long authorId) {
        return jdbc.update(
                "DELETE FROM subscriptions WHERE subscriber_id = ? AND author_id = ?",
                subscriberId,
                authorId
        ) > 0;
    }

    public void refresh(long subscriberId) {
        jdbc.update("""
                INSERT INTO subscription_sets (author_ids)
                SELECT array_agg(author_id ORDER BY author_id)
                FROM subscriptions
                WHERE subscriber_id = ?
                HAVING count(*) > 0
                ON CONFLICT (author_ids) DO NOTHING
                """, subscriberId);
        jdbc.update("""
                INSERT INTO user_subscription (user_id, subscription_id)
                SELECT ?, id
                FROM subscription_sets
                WHERE author_ids = (
                    SELECT array_agg(author_id ORDER BY author_id)
                    FROM subscriptions
                    WHERE subscriber_id = ?
                )
                ON CONFLICT (user_id) DO UPDATE SET subscription_id = EXCLUDED.subscription_id
                """, subscriberId, subscriberId);
        jdbc.update("""
                DELETE FROM user_subscription
                WHERE user_id = ?
                  AND NOT EXISTS (SELECT 1 FROM subscriptions WHERE subscriber_id = ?)
                """, subscriberId, subscriberId);
    }

    public Optional<SubscriptionResponse> findMy(long userId) {
        List<SubscriptionResponse> rows = jdbc.query("""
                SELECT us.subscription_id, ss.author_ids
                FROM user_subscription us
                JOIN subscription_sets ss ON ss.id = us.subscription_id
                WHERE us.user_id = ?
                """, (rs, row) -> new SubscriptionResponse(rs.getLong("subscription_id"), longs(rs.getArray("author_ids"))), userId);
        return rows.stream().findFirst();
    }

    public List<FollowerResponse> followers(long authorId, Page page) {
        return jdbc.query("""
                SELECT subscriber_id, created_at
                FROM subscriptions
                WHERE author_id = ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """, (rs, row) -> new FollowerResponse(
                rs.getLong("subscriber_id"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant()
        ), authorId, page.limit(), page.offset());
    }

    public List<Long> setsContaining(long authorId) {
        return jdbc.query(
                "SELECT id FROM subscription_sets WHERE ? = ANY (author_ids) ORDER BY id",
                (rs, row) -> rs.getLong("id"),
                authorId
        );
    }

    private static List<Long> longs(Array array) throws SQLException {
        if (array == null || array.getArray() == null) {
            return List.of();
        }
        Object raw = array.getArray();
        if (raw instanceof Long[] values) {
            return List.of(values);
        }
        if (raw instanceof Number[] values) {
            return Arrays.stream(values).map(Number::longValue).toList();
        }
        return List.of();
    }

    private static String sqlState(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sql) {
                return sql.getSQLState();
            }
            current = current.getCause();
        }
        return "";
    }
}
