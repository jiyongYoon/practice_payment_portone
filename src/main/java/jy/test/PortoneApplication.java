package jy.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PortoneApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortoneApplication.class, args);
    }

}
