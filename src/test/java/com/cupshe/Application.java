package com.cupshe;

import com.cupshe.restclient.EnableRestClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Application
 *
 * @author zxy
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableRestClient
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }
}
