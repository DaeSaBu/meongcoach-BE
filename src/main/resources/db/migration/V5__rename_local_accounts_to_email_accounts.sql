-- 이메일·비밀번호 로그인 계정의 명칭을 엔티티명(EmailAccount)에 맞춰 local_accounts → email_accounts로 바꾼다.
-- 제약·시퀀스 이름은 DB마다 다르고(V1 생성분과 Flyway 도입 전 ddl-auto 생성분) 애플리케이션이 참조하지 않으므로 그대로 둔다.
ALTER TABLE "local_accounts" RENAME TO "email_accounts";
