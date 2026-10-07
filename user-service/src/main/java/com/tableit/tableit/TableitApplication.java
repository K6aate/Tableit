package com.tableit.tableit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TableitApplication {

	public static void main(String[] args) {
		SpringApplication.run(TableitApplication.class, args);
	}

}
