package com.elma.gohan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GohanApplication {

    public static void main(String[] args) {
        SpringApplication.run(GohanApplication.class, args);
    }
}
