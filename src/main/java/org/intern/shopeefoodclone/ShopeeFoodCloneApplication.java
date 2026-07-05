package org.intern.shopeefoodclone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShopeeFoodCloneApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopeeFoodCloneApplication.class, args);
    }

}
