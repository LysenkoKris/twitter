package com.twitter.sms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.sms.service.UmsClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfig {

    @Bean
    UmsClient umsClient(ObjectMapper objectMapper, AppProperties properties) {
        RestClient client = RestClient.builder().baseUrl(trim(properties.getUmsBaseUrl())).build();
        return new UmsClient(client, objectMapper);
    }

    @Bean
    TransactionTemplate transactionTemplate(PlatformTransactionManager manager) {
        return new TransactionTemplate(manager);
    }

    private static String trim(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
