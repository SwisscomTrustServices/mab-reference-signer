package org.sts.demo.signer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(QtspProperties.class)
@SpringBootApplication
public class SigningDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SigningDemoApplication.class, args);
    }
}
