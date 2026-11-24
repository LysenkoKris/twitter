package com.twitter.mms;

import com.twitter.mms.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class MmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MmsApplication.class, args);
    }
}
