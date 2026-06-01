package com.aikids.care.domain.chat.service;

import com.aikids.care.domain.chat.dto.*;
import com.aikids.care.domain.chat.model.*;
import com.aikids.care.domain.chat.repository.ChatRepository;
import com.aikids.care.domain.chat.repository.ChatDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final GeminiService geminiService;

    // 1. 새로운 상담 세션(빈 방) 생성
    @Transactional
    public Long createChat(ChatCreateRequest request) {
        Chat chat = Chat.builder()
                .childId(request.getChildId())
                .build();
        return chatRepository.save(chat).getId();
    }

    // 2. 메시지 전송 및 AI 답변 받기
    @Transactional
    public String sendMessage(Long chatId, ChatMessageRequest request) {
        // 1) 채팅방 찾기
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 없습니다."));

        // 2) 부모가 보낸 메시지(사진 포함)를 DB에 저장
        ChatDetail userMsg = ChatDetail.builder()
                .chat(chat)
                .role(Role.USER)
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .build();
        chatDetailRepository.save(userMsg);

        // 3) AI에게 물어보고 답변 받기
        String aiContent = geminiService.askQuestion(request.getContent(), request.getImageUrl());

        // 4) AI의 답변을 DB에 저장
        ChatDetail aiMsg = ChatDetail.builder()
                .chat(chat)
                .role(Role.AI)
                .content(aiContent)
                .build();
        chatDetailRepository.save(aiMsg);

        return aiContent;
    }

    // 3. AI 분석 결과 업데이트 (PATCH API)
    @Transactional
    public void updateChatResult(Long chatId, ChatUpdateRequest request) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 없습니다."));

        chat.updateResult(request.getCategory(), request.getRiskLevel());
    }

    // 4. 채팅방 삭제
    @Transactional
    public void deleteChat(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 없습니다."));
        chatRepository.delete(chat);
    }

    public List<ChatDetailResponse> getChatHistory(Long chatId) {

        // 1. 해당 채팅방(chatId)의 대화 내역을 시간순(오래된 것 -> 최신 것)으로 가져옵니다.
        List<ChatDetail> details = chatDetailRepository.findByChatIdOrderByCreatedAtAsc(chatId);

        // 2. DB 엔티티(ChatDetail)를 프론트엔드용 DTO(ChatDetailResponse)로 변환해서 리스트로 묶어 반환합니다.
        return details.stream()
                .map(detail -> new ChatDetailResponse(
                        detail.getId(),
                        detail.getRole(),
                        detail.getContent(),
                        detail.getImageUrl(),
                        detail.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
    // 특정 아이의 상담 방 번호 목록을 최신순으로 조회
    public List<Long> getChatRoomList(Long childId) {
        return chatRepository.findByChildIdOrderByCreatedAtDesc(childId)
                .stream()
                .map(chat -> chat.getId())
                .collect(Collectors.toList());
    }

    // 5. 특정 아이의 모든 상담 목록 조회 (GET /api/children/{childId}/chats)
    public List<ChatListResponse> getChatList(Long childId) {
        return chatRepository.findByChildIdOrderByCreatedAtDesc(childId)
                .stream()
                .map(chat -> ChatListResponse.builder()
                        .chatId(chat.getId())
                        .category(chat.getCategory())
                        .riskLevel(chat.getRiskLevel())
                        .createdAt(chat.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // 6. 특정 날짜에 채팅 기록이 있는지 확인
    public boolean hasChatHistoryForDate(Long childId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        List<Chat> chats = chatRepository.findByChildIdAndCreatedAtBetween(childId, startOfDay, endOfDay);
        return !chats.isEmpty();
    }

    private static final String ANALYSIS_PROMPT_TEMPLATE = """
    너는 소아 건강 상담 내용을 분석하는 전문 AI 의료 보조 시스템이야.
    아래 제공된 대화 내역을 분석하여, 가장 적절한 증상 카테고리와 위험도를 판별해줘.

    [분류 기준 - Category]
    - FEVER: 발열 관련
    - DIGESTIVE: 소화기, 구토, 설사
    - RESPIRATORY: 호흡기, 기침, 콧물
    - SKIN: 피부 발진, 두드러기
    - TRAUMA: 외상, 타박상
    - ETC: 기타 (위 항목에 속하지 않는 경우)

    [분류 기준 - RiskLevel]
    - HOME_CARE: 가정 내 처치 및 경과 관찰 가능
    - CLINIC_VISIT: 가까운 시일 내 외래 진료 권고
    - EMERGENCY_ROOM: 즉시 응급실 방문 요망
    - RE_CONSULT: 정보 부족으로 재상담 필요

    반드시 아래 JSON 형식으로만 답변하고, 다른 부가 설명은 절대 하지 마.
    {
      "category": "선택한 카테고리",
      "riskLevel": "선택한 위험도"
    }
    
    답변할 때 굵게(**), 기울임(*), 헤더(#) 기호를 사용하지 말아줘."
    
    [대화 내역]
    %s
    """;

    @Transactional
    public void analyzeAndCloseConsultation(Long chatId) {
        // 1. 상담방 및 대화 내역 조회
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상담방입니다."));

        List<ChatDetail> history = chatDetailRepository.findByChatIdOrderByCreatedAtAsc(chatId);

        // 2. 대화 내역을 하나의 문자열로 합치기
        StringBuilder conversation = new StringBuilder();
        for (ChatDetail detail : history) {
            conversation.append(detail.getRole()).append(": ").append(detail.getContent()).append("\n");
        }


        // 4. 최종 프롬프트 완성
        String finalPrompt = String.format(ANALYSIS_PROMPT_TEMPLATE, conversation.toString());

        try {
            // 5. Gemini API 호출
            String aiResponse = geminiService.askQuestion(finalPrompt,null);

            // 6. JSON 응답 정제 (마크다운 백틱 제거)
            String cleanJson = aiResponse.replace("```json", "").replace("```", "").trim();

            // 7. JSON 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            AiAnalysisResponse analysisResult = objectMapper.readValue(cleanJson, AiAnalysisResponse.class);

            // 8. String을 Enum으로 변환 및 DB 업데이트 (JPA Dirty Checking)
            Category category = Category.valueOf(analysisResult.getCategory().toUpperCase());
            RiskLevel riskLevel = RiskLevel.valueOf(analysisResult.getRiskLevel().toUpperCase());

            chat.updateAnalysis(category, riskLevel);

        } catch (Exception e) {
            log.error("AI 상담 분석 실패: {}", e.getMessage());
            // 파싱 실패나 예외 발생 시 안전하게 '기타/재상담' 등으로 Fallback 처리
            chat.updateAnalysis(Category.ETC, RiskLevel.RE_CONSULT);
        }
    }
}