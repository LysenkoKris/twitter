package com.twitter.mms;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.twitter.mms.domain.Caller;
import com.twitter.mms.domain.RemoteSubscription;
import com.twitter.mms.service.FeedCache;
import com.twitter.mms.service.SubscribersClient;
import com.twitter.mms.service.UmsClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
class MessagesApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private FeedCache cache;

    @MockitoBean
    private UmsClient users;

    @MockitoBean
    private SubscribersClient subscribers;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM messages");
        cache.clear();
    }

    @Test
    void producerPublishesAndFeedUsesTheSubscriptionCache() throws Exception {
        when(users.caller(2L)).thenReturn(new Caller(2, List.of("PRODUCER")));
        when(subscribers.my("8")).thenReturn(new RemoteSubscription(5L, List.of(2L)));
        when(subscribers.setsContaining(2L)).thenReturn(List.of(5L));

        mockMvc.perform(post("/api/v1/messages")
                        .header("X-Session-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"first\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorId").value(2))
                .andExpect(jsonPath("$.text").value("first"));

        mockMvc.perform(get("/api/v1/messages/feed").header("X-Session-Id", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(5))
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages[0].text").value("first"));

        jdbc.update("INSERT INTO messages (text, author_id) VALUES ('hidden', 2)");

        mockMvc.perform(get("/api/v1/messages/feed").header("X-Session-Id", "8"))
                .andExpect(jsonPath("$.messages", hasSize(1)));

        mockMvc.perform(post("/api/v1/messages")
                        .header("X-Session-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"second\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/messages/feed").header("X-Session-Id", "8"))
                .andExpect(jsonPath("$.messages", hasSize(3)));

        mockMvc.perform(get("/api/v1/messages").param("author_id", "2").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].text").value("second"));
    }

    @Test
    void onlyTheAuthorChangesAMessage() throws Exception {
        when(users.caller(2L)).thenReturn(new Caller(2, List.of("PRODUCER")));
        when(users.caller(9L)).thenReturn(new Caller(9, List.of("PRODUCER")));
        when(subscribers.setsContaining(2L)).thenReturn(List.of());

        MvcResult created = mockMvc.perform(post("/api/v1/messages")
                        .header("X-Session-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"draft\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id = created.getResponse().getContentAsString().replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(put("/api/v1/messages/" + id)
                        .header("X-Session-Id", "9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"stolen\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(put("/api/v1/messages/" + id)
                        .header("X-Session-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"published\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("published"));

        mockMvc.perform(delete("/api/v1/messages/" + id).header("X-Session-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("deleted"));

        mockMvc.perform(delete("/api/v1/messages/" + id).header("X-Session-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void businessAndTransportErrorsStaySeparate() throws Exception {
        when(users.caller(4L)).thenReturn(new Caller(4, List.of("GUEST")));
        when(users.caller(2L)).thenReturn(new Caller(2, List.of("PRODUCER")));
        when(subscribers.setsContaining(2L)).thenReturn(List.of());
        when(subscribers.my("8")).thenReturn(new RemoteSubscription(null, List.of()));

        mockMvc.perform(post("/api/v1/messages")
                        .header("X-Session-Id", "4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"nope\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(post("/api/v1/messages")
                        .header("X-Session-Id", "4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(get("/api/v1/messages/feed").header("X-Session-Id", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(nullValue()))
                .andExpect(jsonPath("$.messages", hasSize(0)));

        mockMvc.perform(get("/api/v1/messages").param("limit", "1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/messages")
                        .header("X-Session-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("text is required"));

        mockMvc.perform(put("/api/v1/messages/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(get("/no-such-route"))
                .andExpect(status().isNotFound());
    }
}
