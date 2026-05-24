package com.aikids.care.infra.gemini;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@ConditionalOnBean(VectorStore.class)
@RequiredArgsConstructor
public class RagSearchService {

    private final VectorStore vectorStore;

    @Cacheable(value = "rag-search", key = "#userMessage")
    public List<Document> search(String userMessage) {
        try {
            return vectorStore.similaritySearch(userMessage);
        } catch (Exception e) {
            log.warn("[RagSearchService] RAG 검색 실패. 원인: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
