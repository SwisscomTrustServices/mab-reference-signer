package org.sts.demo.signer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SigningDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SigningDemoApplication.class, args);
    }
    @Bean
    CommandLineRunner showConfig(org.springframework.core.env.Environment env) {
        return args -> System.out.println("QTSP base-url = " + env.getProperty("qtsp.base-url"));
    }
}
