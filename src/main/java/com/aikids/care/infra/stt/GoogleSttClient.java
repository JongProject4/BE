package com.aikids.care.infra.stt;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class GoogleSttClient {
    private static final Logger log = LoggerFactory.getLogger(GoogleSttClient.class);

    private final String credentialsPath;

    public GoogleSttClient(@Value("${app.stt.credentials-path:}") String credentialsPath) {
        this.credentialsPath = credentialsPath != null ? credentialsPath.trim() : "";
    }

    public String transcribe(byte[] audioData) throws IOException {
        if (audioData == null || audioData.length == 0) {
            return "";
        }

        try (SpeechClient speechClient = openSpeechClient()) {
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.WEBM_OPUS)
                    .setSampleRateHertz(48000)
                    .setLanguageCode("ko-KR")
                    .build();

            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(ByteString.copyFrom(audioData))
                    .build();

            RecognizeResponse response = speechClient.recognize(config, audio);

            return response.getResultsList().stream()
                    .flatMap(result -> result.getAlternativesList().stream())
                    .findFirst()
                    .map(alternative -> alternative.getTranscript().trim())
                    .orElse("");
        }
    }

    private SpeechClient openSpeechClient() throws IOException {
        if (credentialsPath.isEmpty()) {
            return SpeechClient.create();
        }

        Path path = credentialsPath.startsWith("file:")
                ? Path.of(URI.create(credentialsPath))
                : Path.of(credentialsPath);

        if (!Files.exists(path)) {
            throw new IOException("STT credentials file not found: " + path.toAbsolutePath());
        }

        try (InputStream in = Files.newInputStream(path)) {
            log.info("Using Google STT credentials file: {}", path.toAbsolutePath());
            GoogleCredentials credentials = GoogleCredentials.fromStream(in);
            SpeechSettings settings = SpeechSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();
            return SpeechClient.create(settings);
        }
    }
}
