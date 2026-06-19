-- AI 알람 등록 기능 (#feat/ai-alarm-register)
-- chats 테이블에 pendingAlarmDraft(JSON 문자열) 컬럼 추가
-- ddl-auto=validate/none 환경에서 수동 실행 필요. 배포 전 RDS에 적용.

ALTER TABLE chats ADD COLUMN pending_alarm_draft TEXT NULL;
