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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class GeminiApiClient {

    private static final String VOICE_SYSTEM_PROMPT =
            "당신은 소아 응급 전화 상담 전문의입니다. 부모가 아이의 긴급 증상을 음성으로 전달하면 답변합니다.\n\n" +
            "[답변 우선순위]\n" +
            "1. 지금 즉시 해야 할 조치를 첫 문장에 말하세요.\n" +
            "2. 응급실 방문이 필요한지 명확하게 알려주세요.\n\n" +
            "[절대 규칙]\n" +
            "- 3문장을 초과하지 마세요.\n" +
            "- *, #, -, ** 등 마크다운 기호 절대 사용 금지.\n" +
            "- 번호 목록, 글머리 기호 목록 절대 금지.\n" +
            "- 공감보다 정확한 정보를 먼저 전달하세요.\n" +
            "- 자연스러운 대화체로만 답하세요.\n" +
            "- 확정적인 의료 진단은 절대 내리지 마세요.\n" +
            "- 아이 정보(병력/알러지)가 있으면 반드시 고려하세요.";

    private static final String TEXT_SYSTEM_PROMPT =
            "당신은 친절하고 전문적인 소아 건강 상담 AI 보조입니다. 부모가 아이의 증상을 텍스트로 질문하면 답변합니다.\n\n" +
            "[답변 원칙]\n" +
            "1. 부모의 걱정에 먼저 공감하세요.\n" +
            "2. 가정에서 할 수 있는 조치를 단계별로 자세히 안내하세요.\n" +
            "3. 심각한 증상이면 병원 방문을 권고하세요.\n" +
            "4. 아이 정보(병력/알러지)가 있으면 반드시 고려하세요.\n\n" +
            "[형식 규칙]\n" +
            "- 번호(1. 2. 3.)나 단계별 설명을 활용해 답변을 구조화하세요.\n" +
            "- *, #, ** 등 마크다운 기호는 사용하지 마세요.\n" +
            "- 확정적인 의료 진단은 절대 내리지 마세요.";

    private static final String SUMMARY_PROMPT =
            "다음은 부모와 소아 응급 상담 AI 간의 대화입니다. " +
            "아이의 증상, 상태, 조치 내용을 중심으로 3문장 이내로 요약해주세요.";

    private final ChatModel chatModel;
    private final RagSearchService ragSearchService;

    public GeminiApiClient(ChatModel chatModel, RagSearchService ragSearchService) {
        this.chatModel = chatModel;
        this.ragSearchService = ragSearchService;
    }

    public String askText(String userMessage, List<Map<String, String>> history, String interimSummary, String childInfo) {
        return ask(userMessage, history, interimSummary, childInfo, TEXT_SYSTEM_PROMPT);
    }

    public String askVoice(String userMessage, List<Map<String, String>> history, String interimSummary, String childInfo) {
        return ask(userMessage, history, interimSummary, childInfo, VOICE_SYSTEM_PROMPT);
    }

    private String ask(String userMessage, List<Map<String, String>> history, String interimSummary, String childInfo, String basePrompt) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(buildSystemPrompt(basePrompt, userMessage, interimSummary, childInfo)));
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

    public String summarize(List<Map<String, String>> history, String previousSummary) {
        String conversationText = history.stream()
                .map(m -> m.get("role") + ": " + m.get("content"))
                .reduce("", (a, b) -> a + "\n" + b);

        String promptBody = (previousSummary != null && !previousSummary.isBlank())
                ? SUMMARY_PROMPT + "\n\n이전 요약:\n" + previousSummary + "\n\n추가 대화:\n" + conversationText
                : SUMMARY_PROMPT + "\n\n" + conversationText;

        List<Message> messages = List.of(
                new SystemMessage(TEXT_SYSTEM_PROMPT),
                new UserMessage(promptBody)
        );
        return call(new Prompt(messages));
    }

    private String buildSystemPrompt(String basePrompt, String userMessage, String interimSummary, String childInfo) {
        StringBuilder base = new StringBuilder(basePrompt);

        if (childInfo != null && !childInfo.isBlank()) {
            base.append("\n\n").append(childInfo);
        }

        if (interimSummary != null && !interimSummary.isBlank()) {
            base.append("\n\n[이전 대화 요약]\n").append(interimSummary);
        }

        if (ragSearchService == null) {
            return base.toString();
        }
        List<Document> docs = ragSearchService.search(userMessage);
        if (docs.isEmpty()) {
            return base.toString();
        }
        String ragContext = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
        return base.append("\n\n참고 의료 가이드라인:\n").append(ragContext).toString();
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
