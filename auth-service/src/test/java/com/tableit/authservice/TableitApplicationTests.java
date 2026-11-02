package com.tableit.authservice;

import com.tableit.authservice.service.TestJwtServices;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TableitApplicationTests {

	@DynamicPropertySource
	static void jwtKeys(DynamicPropertyRegistry registry) {
		TestJwtServices.registerJwtKeyProperties(registry);
	}

	@Test
	void contextLoads() {
	}
}
