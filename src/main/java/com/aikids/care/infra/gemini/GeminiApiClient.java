package com.aikids.care.infra.gemini;

import com.aikids.care.global.error.CustomException;
import com.aikids.care.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class GeminiApiClient {

    private static final String SYSTEM_PROMPT =
            "당신은 친절하고 전문적인 '소아 응급 상담 AI 보조'입니다. " +
            "부모가 아이의 증상을 말하면, 1) 공감하고 안심시킬 것, " +
            "2) 가정에서 할 수 있는 응급 처치법을 안내할 것, " +
            "3) 심각한 경우 즉시 응급실이나 소아과 방문을 권고할 것. " +
            "절대 확정적인 의료 진단을 내리지 마세요.";

    private static final String SUMMARY_PROMPT =
            "다음은 부모와 소아 응급 상담 AI 간의 대화입니다. " +
            "아이의 증상, 상태, 조치 내용을 중심으로 3문장 이내로 요약해주세요.";

    private final ChatModel chatModel;
    // local 프로파일에서는 Chroma가 없으므로 null 허용
    private final VectorStore vectorStore;

    public GeminiApiClient(ChatModel chatModel, VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    public String ask(String userMessage, List<Map<String, String>> history) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(buildSystemPrompt(userMessage)));
        for (Map<String, String> turn : history) {
            String role = turn.get("role");
            String content = turn.get("content");
            if ("user".equals(role)) {
                messages.add(new UserMessage(content));
            } else {
                messages.add(new AssistantMessage(content));
            }
        }
        messages.add(new UserMessage(userMessage));
        return call(new Prompt(messages));
    }

    public String summarize(List<Map<String, String>> history) {
        String conversationText = history.stream()
                .map(m -> m.get("role") + ": " + m.get("content"))
                .reduce("", (a, b) -> a + "\n" + b);

        List<Message> messages = List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(SUMMARY_PROMPT + "\n\n" + conversationText)
        );
        return call(new Prompt(messages));
    }

    // 유사 의료 가이드라인 문서를 검색해 시스템 프롬프트에 컨텍스트로 추가
    // Chroma 장애 시 기본 프롬프트로 폴백
    private String buildSystemPrompt(String userMessage) {
        if (vectorStore == null) {
            return SYSTEM_PROMPT;
        }
        try {
            List<Document> docs = vectorStore.similaritySearch(userMessage);
            if (docs.isEmpty()) {
                return SYSTEM_PROMPT;
            }
            String ragContext = docs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n"));
            return SYSTEM_PROMPT + "\n\n참고 의료 가이드라인:\n" + ragContext;
        } catch (Exception e) {
            log.warn("[GeminiApiClient] RAG 검색 실패, 기본 프롬프트로 폴백. 원인: {}", e.getMessage());
            return SYSTEM_PROMPT;
        }
    }

    private String call(Prompt prompt) {
        try {
            return chatModel.call(prompt).getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("[GeminiApiClient] Gemini 호출 실패", e);
            throw new CustomException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }
}
