package com.virtualbank.logging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing // Add this annotation
public class LoggingServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(LoggingServiceApplication.class, args);
	}
}