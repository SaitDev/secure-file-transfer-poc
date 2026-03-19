package pes.poc.a;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SystemAApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemAApplication.class, args);
    }

}
