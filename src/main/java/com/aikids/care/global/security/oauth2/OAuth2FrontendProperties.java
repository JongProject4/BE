package com.aikids.care.global.security.oauth2;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth2")
public record OAuth2FrontendProperties(String frontendRedirectUri) {
}
