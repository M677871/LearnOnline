package com.csis231.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = {ApiApplication.class, TestMailConfig.class})
@Import(TestMailConfig.class)
class ApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
