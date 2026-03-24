package com.aikids.care;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"spring.autoconfigure.exclude=org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration"
})
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
