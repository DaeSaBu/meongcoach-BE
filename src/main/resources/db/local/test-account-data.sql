-- 스토어 심사(Google Play·App Store)용 이메일 로그인 테스트 계정. local 프로파일(PostgreSQL, create-drop)에서만 적재된다.
-- 로그인: POST /api/auth/login/local { "email": "review@meongcoach.com", "password": "meongcoach-review" }
-- password_hash는 BCrypt. 재생성: htpasswd -bnBC 10 "" '비밀번호' | tr -d ':\n'  ($2y$ 접두어도 BCryptPasswordEncoder가 허용)
-- 온보딩 전 상태(ONBOARDING_MEMBER, 프로필 없음)로 두어 심사자가 앱에서 온보딩까지 직접 진행한다.
-- 테이블은 기동마다 새로 생성되므로 id를 고정하고 시퀀스를 뒤로 맞춘다. dev/prod 수동 등록은 docs/security.md 참고.

BEGIN;

INSERT INTO "users" ("id", "role", "status", "created_at", "updated_at") VALUES
	(1, 'ONBOARDING_MEMBER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT ("id") DO NOTHING;

INSERT INTO "local_accounts" ("id", "user_id", "email", "password_hash", "created_at", "updated_at") VALUES
	(1, 1, 'review@meongcoach.com', '$2y$10$1quJ3YNyS9soFGSmbBXw2uLtBbSrP4SGlqMmIoa3R7JsbFxh.XoiS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT ("id") DO NOTHING;

SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX("id") FROM "users"), true);
SELECT setval(pg_get_serial_sequence('local_accounts', 'id'), (SELECT MAX("id") FROM "local_accounts"), true);

COMMIT;
