package com.twitter.mms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.mms.service.SubscribersClient;
import com.twitter.mms.service.UmsClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfig {

    @Bean
    UmsClient umsClient(ObjectMapper objectMapper, AppProperties properties) {
        return new UmsClient(client(properties.getUmsBaseUrl()), objectMapper);
    }

    @Bean
    SubscribersClient subscribersClient(ObjectMapper objectMapper, AppProperties properties) {
        return new SubscribersClient(client(properties.getSubscribersBaseUrl()), objectMapper);
    }

    private static RestClient client(String baseUrl) {
        String root = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return RestClient.builder().baseUrl(root).build();
    }
}
