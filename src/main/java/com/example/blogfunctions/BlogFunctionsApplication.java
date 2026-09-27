package com.example.blogfunctions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BlogFunctionsApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogFunctionsApplication.class, args);
    }
}
