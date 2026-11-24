package com.twitter.mms.repository;

import com.twitter.mms.contracts.FeedItem;
import com.twitter.mms.contracts.MessageView;
import com.twitter.mms.domain.Page;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MessageRepository {

    private final JdbcTemplate jdbc;
    private final RowMapper<MessageView> messageMapper = (rs, row) -> new MessageView(
            rs.getLong("id"),
            rs.getString("text"),
            rs.getLong("author_id"),
            rs.getObject("created_at", OffsetDateTime.class).toInstant(),
            rs.getObject("updated_at", OffsetDateTime.class).toInstant()
    );

    public MessageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public MessageView insert(long authorId, String text) {
        return jdbc.queryForObject("""
                INSERT INTO messages (text, author_id)
                VALUES (?, ?)
                RETURNING id, text, author_id, created_at, updated_at
                """, messageMapper, text, authorId);
    }

    public Optional<MessageView> find(long id) {
        List<MessageView> rows = jdbc.query("""
                SELECT id, text, author_id, created_at, updated_at
                FROM messages
                WHERE id = ?
                """, messageMapper, id);
        return rows.stream().findFirst();
    }

    public Optional<MessageView> update(long id, long authorId, String text) {
        List<MessageView> rows = jdbc.query("""
                UPDATE messages
                SET text = ?, updated_at = NOW()
                WHERE id = ? AND author_id = ?
                RETURNING id, text, author_id, created_at, updated_at
                """, messageMapper, text, id, authorId);
        return rows.stream().findFirst();
    }

    public boolean delete(long id, long authorId) {
        return jdbc.update("DELETE FROM messages WHERE id = ? AND author_id = ?", id, authorId) > 0;
    }

    public List<MessageView> byAuthor(long authorId, Page page) {
        return jdbc.query("""
                SELECT id, text, author_id, created_at, updated_at
                FROM messages
                WHERE author_id = ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """, messageMapper, authorId, page.limit(), page.offset());
    }

    public List<FeedItem> feed(List<Long> authorIds, Page page) {
        if (authorIds.isEmpty()) {
            return List.of();
        }
        return jdbc.query(connection -> {
            var statement = connection.prepareStatement("""
                    SELECT id, author_id, text, created_at
                    FROM messages
                    WHERE author_id = ANY (?)
                    ORDER BY created_at DESC
                    LIMIT ? OFFSET ?
                    """);
            statement.setArray(1, connection.createArrayOf("bigint", authorIds.toArray()));
            statement.setInt(2, page.limit());
            statement.setInt(3, page.offset());
            return statement;
        }, (rs, row) -> new FeedItem(
                rs.getLong("id"),
                rs.getLong("author_id"),
                rs.getString("text"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant()
        ));
    }
}
