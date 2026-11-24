package com.twitter.mms.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String umsBaseUrl = "http://localhost:8080";
    private String subscribersBaseUrl = "http://localhost:8085";
    private final Cors cors = new Cors();

    public String getUmsBaseUrl() {
        return umsBaseUrl;
    }

    public void setUmsBaseUrl(String umsBaseUrl) {
        this.umsBaseUrl = umsBaseUrl;
    }

    public String getSubscribersBaseUrl() {
        return subscribersBaseUrl;
    }

    public void setSubscribersBaseUrl(String subscribersBaseUrl) {
        this.subscribersBaseUrl = subscribersBaseUrl;
    }

    public Cors getCors() {
        return cors;
    }

    public static class Cors {

        private List<String> allowedOrigins = new ArrayList<>(List.of(
                "http://localhost:3000",
                "http://localhost:5173"
        ));
        private List<String> allowedMethods = new ArrayList<>(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }
    }
}
