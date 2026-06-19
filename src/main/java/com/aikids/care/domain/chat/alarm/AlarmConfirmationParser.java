package com.aikids.care.domain.chat.alarm;

import java.util.regex.Pattern;

public final class AlarmConfirmationParser {

    public enum Decision {
        CONFIRM,
        CANCEL,
        AMBIGUOUS
    }

    // 한 단어처럼 등장하는 긍정/부정 표현. \W 경계로 부분 문자열 오인식 방지 (예: "아냐" 안에 "아"만 매칭 금지).
    private static final Pattern CONFIRM = Pattern.compile(
            "(^|[\\s.,!?])(네|예|응|어|맞아|좋아|좋습니다|등록해줘|등록할래|확인|오케이|ok)([\\s.,!?]|$)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CANCEL = Pattern.compile(
            "(^|[\\s.,!?])(아니|아뇨|아니요|취소|싫어|아냐|관둬|그만|됐어|괜찮아)([\\s.,!?]|$)"
    );

    private AlarmConfirmationParser() {
    }

    public static Decision parse(String message) {
        if (message == null || message.isBlank()) return Decision.AMBIGUOUS;
        boolean confirm = CONFIRM.matcher(message).find();
        boolean cancel = CANCEL.matcher(message).find();
        if (confirm && cancel) return Decision.AMBIGUOUS;
        if (cancel) return Decision.CANCEL;
        if (confirm) return Decision.CONFIRM;
        return Decision.AMBIGUOUS;
    }
}
