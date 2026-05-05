package com.aikids.care.domain.auth.controller;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
public class AuthTokenViewController {

	@GetMapping(value = { "/auth/token", "/login-success" }, produces = MediaType.TEXT_HTML_VALUE)
	public String showToken(@RequestParam(name = "token", required = false) String token) {
		if (!StringUtils.hasText(token)) {
			return """
					<!doctype html>
					<html lang="ko">
					<head><meta charset="UTF-8"><title>JWT Token</title></head>
					<body>
					  <h2>JWT Token</h2>
					  <p>token 쿼리 파라미터가 없습니다.</p>
					  <p>아래 링크로 구글 로그인부터 시작하면 토큰이 붙은 상태로 다시 이 페이지로 돌아옵니다.</p>
					  <p><a href="/oauth2/authorization/google">구글 로그인 시작</a></p>
					</body>
					</html>
					""";
		}

		String safeToken = HtmlUtils.htmlEscape(token);
		return """
				<!doctype html>
				<html lang="ko">
				<head><meta charset="UTF-8"><title>JWT Token</title></head>
				<body>
				  <h2>JWT Token 발급 성공</h2>
				  <p>아래 토큰을 Postman Authorization Bearer Token에 넣어 사용하세요.</p>
				  <textarea rows="8" cols="120" readonly>%s</textarea>
				</body>
				</html>
				""".formatted(safeToken);
	}
}
