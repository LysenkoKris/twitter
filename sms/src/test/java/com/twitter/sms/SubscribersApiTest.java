package com.twitter.sms;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.sms.domain.Author;
import com.twitter.sms.domain.Caller;
import com.twitter.sms.exception.RequestFailure;
import com.twitter.sms.service.UmsClient;
import java.util.List;
import org.junit.jupiter.api.Assertions;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
class SubscribersApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UmsClient users;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM user_subscription");
        jdbc.update("DELETE FROM subscriptions");
        jdbc.update("DELETE FROM subscription_sets");
    }

    @Test
    void subscribersShareASetUntilTheAuthorListChanges() throws Exception {
        when(users.caller(3L)).thenReturn(new Caller(3, List.of("SUBSCRIBER")));
        when(users.caller(4L)).thenReturn(new Caller(4, List.of("SUBSCRIBER")));
        when(users.author(9)).thenReturn(new Author(9, List.of("PRODUCER")));
        when(users.author(10)).thenReturn(new Author(10, List.of("PRODUCER")));

        JsonNode first = objectMapper.readTree(mockMvc.perform(post("/api/v1/subscribers")
                        .header("X-Session-Id", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":9}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authors[0]").value(9))
                .andReturn().getResponse().getContentAsString());
        JsonNode second = objectMapper.readTree(mockMvc.perform(post("/api/v1/subscribers")
                        .header("X-Session-Id", "4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":9}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        Assertions.assertEquals(first.get("subscriptionId").asLong(), second.get("subscriptionId").asLong());

        mockMvc.perform(get("/api/v1/subscribers/my").header("X-Session-Id", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authors[0]").value(9));
        mockMvc.perform(get("/api/v1/subscribers/9/followers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subscriberId").exists())
                .andExpect(jsonPath("$[1].subscriberId").exists());
        mockMvc.perform(get("/api/v1/subscribers/sets").param("author_id", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").isNumber());

        mockMvc.perform(post("/api/v1/subscribers")
                        .header("X-Session-Id", "4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authors[0]").value(9))
                .andExpect(jsonPath("$.authors[1]").value(10));

        mockMvc.perform(delete("/api/v1/subscribers/9").header("X-Session-Id", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("unsubscribed"));
        mockMvc.perform(delete("/api/v1/subscribers/10").header("X-Session-Id", "4"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/subscribers/9").header("X-Session-Id", "4"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/subscribers/my").header("X-Session-Id", "3"))
                .andExpect(jsonPath("$.subscriptionId").value(nullValue()))
                .andExpect(jsonPath("$.authors", empty()));
    }

    @Test
    void businessRulesStayOnHttp200() throws Exception {
        when(users.caller(1L)).thenReturn(new Caller(3, List.of("GUEST")));
        when(users.caller(3L)).thenReturn(new Caller(3, List.of("SUBSCRIBER")));
        when(users.author(3)).thenReturn(new Author(3, List.of("SUBSCRIBER", "PRODUCER")));
        when(users.author(8)).thenReturn(new Author(8, List.of("GUEST")));
        when(users.author(9)).thenReturn(new Author(9, List.of("PRODUCER")));
        when(users.author(404)).thenThrow(RequestFailure.business(404, "user not found"));

        mockMvc.perform(post("/api/v1/subscribers")
                        .header("X-Session-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(post("/api/v1/subscribers")
                        .header("X-Session-Id", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("cannot subscribe to yourself"));

        mockMvc.perform(post("/api/v1/subscribers")
                        .header("X-Session-Id", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("author is not a producer"));

        mockMvc.perform(post("/api/v1/subscribers")
                        .header("X-Session-Id", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":9}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/subscribers")
                        .header("X-Session-Id", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));

        mockMvc.perform(post("/api/v1/subscribers")
                        .header("X-Session-Id", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":404}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(get("/api/v1/subscribers/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void transportErrorsKeepTheHttpStatus() throws Exception {
        when(users.caller(3L)).thenThrow(RequestFailure.transport(502, "users service unavailable"));

        mockMvc.perform(post("/api/v1/subscribers")
                        .header("X-Session-Id", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":9}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(502));

        mockMvc.perform(post("/api/v1/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/subscribers/sets"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/no-such-route"))
                .andExpect(status().isNotFound());
    }
}
