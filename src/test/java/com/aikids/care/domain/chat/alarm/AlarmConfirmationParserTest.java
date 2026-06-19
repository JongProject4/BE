package com.aikids.care.domain.chat.alarm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static com.aikids.care.domain.chat.alarm.AlarmConfirmationParser.Decision;

class AlarmConfirmationParserTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "네",
            "네.",
            "응",
            "예",
            "예예",
            "맞아",
            "좋아 등록해줘",
            "오케이",
            "ok",
            "Ok",
            "확인",
            "그래",
            "그래요",
            "그럼",
            "그러죠",
            "맞습니다",
            "등록",
            "등록해",
            "등록해주세요",
            "오키",
            "ㅇㅋ",
            "ㅇㅇ",
            "yes"
    })
    @DisplayName("긍정 표현 → CONFIRM")
    void confirmPhrases(String message) {
        assertThat(AlarmConfirmationParser.parse(message)).isEqualTo(Decision.CONFIRM);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "아니",
            "아니요",
            "취소",
            "싫어",
            "관둬",
            "그만"
    })
    @DisplayName("부정 표현 → CANCEL")
    void cancelPhrases(String message) {
        assertThat(AlarmConfirmationParser.parse(message)).isEqualTo(Decision.CANCEL);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "음 잘 모르겠는데",
            "그러게 어떻게 할까",
            "타이레놀 5ml 24시간마다",
            ""
    })
    @DisplayName("긍정·부정 단어 없거나 빈 메시지 → AMBIGUOUS")
    void ambiguousPhrases(String message) {
        assertThat(AlarmConfirmationParser.parse(message)).isEqualTo(Decision.AMBIGUOUS);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "네 아니 잘 모르겠어",
            "응 취소할래"
    })
    @DisplayName("긍정·부정 둘 다 포함 → AMBIGUOUS (드러내고 다시 묻기)")
    void contradictoryPhrases(String message) {
        assertThat(AlarmConfirmationParser.parse(message)).isEqualTo(Decision.AMBIGUOUS);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "내일이라고 했어",
            "예방접종 알림"
    })
    @DisplayName("부분 문자열 오인식 방지 — '내일'·'예방접종'은 CONFIRM이 아님")
    void substringFalsePositiveGuard(String message) {
        Decision result = AlarmConfirmationParser.parse(message);
        assertThat(result).isNotEqualTo(Decision.CONFIRM);
    }
}
