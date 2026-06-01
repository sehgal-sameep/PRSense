package com.codewithsam.prsense;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PrSenseApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrSenseApplication.class, args);
    }
}
