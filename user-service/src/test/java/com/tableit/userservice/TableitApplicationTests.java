package com.tableit.userservice;

import com.tableit.userservice.security.TestJwt;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TableitApplicationTests {

	@DynamicPropertySource
	static void jwtKey(DynamicPropertyRegistry registry) {
		TestJwt.registerAccessPublicKey(registry);
	}

	@Test
	void contextLoads() {
	}
}
