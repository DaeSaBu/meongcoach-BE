-- dev DB에 psql로 직접 실행한다. Flyway 자동 마이그레이션에는 포함하지 않는다.
-- password_hash에는 BCrypt 해시를 전달한다. 평문 비밀번호는 저장하지 않는다.
-- 기본 이메일은 dev-test1@example.com부터 dev-test10@example.com까지다.
-- 재실행 시 기존 이메일은 건너뛰며 비밀번호·회원 상태를 변경하지 않는다.
-- 해시 생성(비밀번호 대화형 입력): TEST_PASSWORD_HASH=$(htpasswd -nBC 10 "" | tr -d ':\n')
-- 실행: psql "$DEV_DATABASE_URL" -v password_hash="$TEST_PASSWORD_HASH" -f scripts/dev/create-test-accounts.sql
-- 범위 변경: 위 명령에 -v start_number=11 -v account_count=10을 추가한다.
\set ON_ERROR_STOP on

\if :{?password_hash}
\else
	\prompt 'BCrypt 비밀번호 해시: ' password_hash
\endif
\if :{?start_number}
\else
	\set start_number 1
\endif
\if :{?account_count}
\else
	\set account_count 10
\endif

BEGIN;

CREATE TEMP TABLE test_account_config (
	password_hash TEXT NOT NULL
		CHECK (password_hash ~ '^\$2[aby]\$(0[4-9]|[12][0-9]|3[01])\$[./A-Za-z0-9]{53}$'),
	start_number INTEGER NOT NULL CHECK (start_number > 0),
	account_count INTEGER NOT NULL CHECK (account_count BETWEEN 1 AND 1000)
) ON COMMIT DROP;

INSERT INTO test_account_config (password_hash, start_number, account_count)
VALUES (:'password_hash', :'start_number', :'account_count');

CREATE TEMP TABLE created_test_accounts (
	user_id BIGINT NOT NULL,
	email TEXT NOT NULL
) ON COMMIT DROP;

DO $$
DECLARE
	config RECORD;
	account_number BIGINT;
	account_email TEXT;
	new_user_id BIGINT;
BEGIN
	SELECT * INTO STRICT config FROM test_account_config;

	FOR account_number IN
		SELECT generate_series(config.start_number::BIGINT, config.start_number::BIGINT + config.account_count - 1)
	LOOP
		account_email := format('dev-test%s@example.com', account_number);

		IF EXISTS (SELECT 1 FROM email_accounts WHERE email = account_email) THEN
			CONTINUE;
		END IF;

		INSERT INTO users (role, status, created_at, updated_at)
		VALUES ('ONBOARDING_USER', 'ACTIVE', now(), now())
		RETURNING id INTO new_user_id;

		INSERT INTO email_accounts (user_id, email, password_hash, created_at, updated_at)
		VALUES (new_user_id, account_email, config.password_hash, now(), now());

		INSERT INTO created_test_accounts (user_id, email)
		VALUES (new_user_id, account_email);
	END LOOP;
END;
$$;

SELECT user_id, email FROM created_test_accounts ORDER BY user_id;

COMMIT;
