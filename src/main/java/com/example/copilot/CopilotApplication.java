package com.example.copilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
//@EnableJpaAuditing // Disable auditing for test context issues
public class CopilotApplication {

	public static void main(String[] args) {
		SpringApplication.run(CopilotApplication.class, args);
	}

}
