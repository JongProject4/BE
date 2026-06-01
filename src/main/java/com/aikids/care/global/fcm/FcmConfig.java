package com.aikids.care.global.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.fcm.credentials-path")
public class FcmConfig {

    @Value("${app.fcm.credentials-path}")
    private String credentialsPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream(credentialsPath))
                .createScoped("https://www.googleapis.com/auth/firebase.messaging");
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        log.info("Firebase initialized with credentials: {}", credentialsPath);
        return FirebaseApp.initializeApp(options);
    }
}
