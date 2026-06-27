package dev.taskflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TaskflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskflowApplication.class, args);
    }
}
