package com.aikids.care.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class RagConfig {

    @Value("${spring.ai.google.genai.api-key}")
    private String apiKey;

    @Bean
    public EmbeddingModel embeddingModel() {
        return new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                RestTemplate restTemplate = new RestTemplate();
                String url = "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key=" + apiKey;
                List<Embedding> embeddings = new ArrayList<>();
                for (int i = 0; i < request.getInstructions().size(); i++) {
                    String text = request.getInstructions().get(i);
                    Map<String, Object> body = Map.of(
                            "model", "models/text-embedding-004",
                            "content", Map.of("parts", List.of(Map.of("text", text)))
                    );
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                    Map response = restTemplate.postForObject(url, entity, Map.class);
                    Map embeddingMap = (Map) response.get("embedding");
                    List<Double> values = (List<Double>) embeddingMap.get("values");
                    float[] floatValues = new float[values.size()];
                    for (int j = 0; j < values.size(); j++) {
                        floatValues[j] = values.get(j).floatValue();
                    }
                    embeddings.add(new Embedding(floatValues, i));
                }
                return new EmbeddingResponse(embeddings);
            }

            @Override
            public float[] embed(Document document) {
                EmbeddingRequest request = new EmbeddingRequest(List.of(document.getText()), null);
                return call(request).getResults().get(0).getOutput();
            }

            @Override
            public int dimensions() {
                return 768;
            }
        };
    }

    @Bean
    public ChromaApi chromaApi() {
        return new ChromaApi(
                "http://localhost:8000",
                RestClient.builder(),
                new ObjectMapper()
        );
    }

    // @Lazy 어노테이션을 붙여서 서버 시작 시점의 강제 초기화 에러를 회피합니다.
    @Bean
    @Lazy
    public ChromaVectorStore vectorStore(EmbeddingModel embeddingModel, ChromaApi chromaApi) {
        String collection = "medical-guidelines";

        // Spring AI 객체가 생성되기 전, 가장 원시적이고 확실한 v1 API로 컬렉션 강제 생성을 찔러봅니다.
        try {
            RestTemplate rt = new RestTemplate();
            String url = "http://localhost:8000/api/v1/collections";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = "{\"name\":\"" + collection + "\"}";
            rt.postForObject(url, new HttpEntity<>(body, headers), String.class);
            System.out.println(">>> Chroma DB: 컬렉션 검증/생성 시도 완료");
        } catch (Exception createEx) {
            // 컬렉션이 이미 존재해서 발생하는 400, 409 등의 에러는 안전하게 무시합니다.
        }

        return ChromaVectorStore.builder(chromaApi, embeddingModel)
                .collectionName(collection)
                .initializeSchema(false)
                .build();
    }
}