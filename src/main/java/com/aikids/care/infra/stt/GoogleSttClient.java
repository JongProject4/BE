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
import java.util.Locale;

@Component
public class GoogleSttClient {
    private static final Logger log = LoggerFactory.getLogger(GoogleSttClient.class);

    private final String credentialsPath;

    public GoogleSttClient(@Value("${app.stt.credentials-path:}") String credentialsPath) {
        this.credentialsPath = credentialsPath != null ? credentialsPath.trim() : "";
    }

    public String transcribe(byte[] audioData) throws IOException {
        return transcribe(audioData, null, null);
    }

    /**
     * @param contentType   multipart Content-Type (예: audio/webm, audio/mpeg)
     * @param filename      원본 파일명 (확장자 기반 fallback)
     */
    public String transcribe(byte[] audioData, String contentType, String filename) throws IOException {
        if (audioData == null || audioData.length == 0) {
            return "";
        }

        RecognitionConfig config = buildConfig(contentType, filename);
        log.info("[STT] encoding={}, sampleRate={}, contentType={}, filename={}, bytes={}",
                config.getEncoding(), config.getSampleRateHertz(), contentType, filename, audioData.length);

        try (SpeechClient speechClient = openSpeechClient()) {
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(ByteString.copyFrom(audioData))
                    .build();

            RecognizeResponse response = speechClient.recognize(config, audio);

            return response.getResultsList().stream()
                    .flatMap(result -> result.getAlternativesList().stream())
                    .findFirst()
                    .map(alternative -> alternative.getTranscript().trim())
                    .orElse("");
        } catch (RuntimeException e) {
            throw new IOException("Google STT failed: " + e.getMessage(), e);
        }
    }

    private RecognitionConfig buildConfig(String contentType, String filename) {
        String mime = contentType != null ? contentType.toLowerCase(Locale.ROOT).split(";")[0].trim() : "";
        String lowerName = filename != null ? filename.toLowerCase(Locale.ROOT) : "";

        if (mime.contains("webm") || lowerName.endsWith(".webm")) {
            return RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.WEBM_OPUS)
                    .setSampleRateHertz(48_000)
                    .setLanguageCode("ko-KR")
                    .build();
        }
        if (mime.contains("mpeg") || mime.contains("mp3") || lowerName.endsWith(".mp3")) {
            return RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.MP3)
                    .setLanguageCode("ko-KR")
                    .build();
        }
        if (mime.contains("wav") || lowerName.endsWith(".wav")) {
            return RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(16_000)
                    .setLanguageCode("ko-KR")
                    .build();
        }
        if (mime.contains("ogg") || lowerName.endsWith(".ogg")) {
            return RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.OGG_OPUS)
                    .setSampleRateHertz(48_000)
                    .setLanguageCode("ko-KR")
                    .build();
        }
        if (mime.contains("flac") || lowerName.endsWith(".flac")) {
            return RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.FLAC)
                    .setSampleRateHertz(16_000)
                    .setLanguageCode("ko-KR")
                    .build();
        }

        // 기본값(과거 동작): webm/opus — mp3/m4a 업로드 시 InvalidArgument 로 500 나던 원인
        log.warn("[STT] Unknown audio type contentType='{}', filename='{}'. Falling back to WEBM_OPUS.",
                contentType, filename);
        return RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.WEBM_OPUS)
                .setSampleRateHertz(48_000)
                .setLanguageCode("ko-KR")
                .build();
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
