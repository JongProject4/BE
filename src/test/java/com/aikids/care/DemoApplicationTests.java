package com.aikids.care;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.ai.vectorstore.VectorStore;

@SpringBootTest
class DemoApplicationTests {

	// 테스트 환경에서는 실제 Chroma DB나 외부 AI API에 연결하지 않도록
	// 가짜 객체(Mock)를 주입하여 애플리케이션 컨텍스트 로드 에러를 방지합니다.
	@MockitoBean
	private VectorStore vectorStore;

	@Test
	void contextLoads() {
		// 이 빈 공간은 정상입니다. 애플리케이션이 에러 없이 켜지는지만 확인합니다.
	}
}