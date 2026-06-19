package com.aikids.care;

import com.aikids.care.global.security.jwt.JwtProperties;
import com.aikids.care.global.security.oauth2.OAuth2FrontendProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.TimeZone;


@SpringBootApplication
@EnableConfigurationProperties({ JwtProperties.class, OAuth2FrontendProperties.class })
public class DemoApplication {

	public static void main(String[] args) {
		// 서비스 사용자(한국)와 DB 저장값(KST wall-clock)을 정렬하기 위해 JVM 기본 타임존을 KST로 고정.
		// LocalDateTime.now()가 KST를 반환하게 되어 mysql-connector-j의 JVM TZ→serverTimezone 변환 폭이 0이 됨.
		// 즉, 기존 toStorage(KST→UTC) 보상 코드가 불필요해지며 DB/엔티티 값이 직관적으로 일치한다.
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		SpringApplication.run(DemoApplication.class, args);
	}
}