package com.cheat.exam;

import com.cheat.exam.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class CheatApplication {

    //
    public static void main(String[] args) {
        SpringApplication.run(CheatApplication.class, args);
    }
}
