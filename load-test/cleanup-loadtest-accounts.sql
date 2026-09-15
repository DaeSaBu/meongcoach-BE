-- 부하 테스트가 만든 계정과 그 하위 데이터를 지운다. 대상은 이메일이 lt-…@meongcoach.test 인 로컬 계정 전부다.
-- dev DB에 psql로 직접 실행한다(운영자 DB 접근 절차는 docs/security.md 참고). 로컬은 재기동(create-drop)으로 초기화되므로 필요 없다.
-- 실행 예: psql "$DEV_DATABASE_URL" -f load-test/cleanup-loadtest-accounts.sql
-- 삭제 순서는 FK 역순이다. 소프트 삭제된 강아지(deleted_at)도 함께 지운다.

BEGIN;

CREATE TEMP TABLE loadtest_users ON COMMIT DROP AS
SELECT user_id FROM local_accounts WHERE email LIKE 'lt-%@meongcoach.test';

DELETE FROM refresh_tokens        WHERE user_id IN (SELECT user_id FROM loadtest_users);
DELETE FROM user_lesson_progress  WHERE user_id IN (SELECT user_id FROM loadtest_users);
DELETE FROM user_selected_topic   WHERE user_id IN (SELECT user_id FROM loadtest_users);
DELETE FROM dog_personalities     WHERE dog_id IN (SELECT id FROM dogs WHERE user_id IN (SELECT user_id FROM loadtest_users));
DELETE FROM dogs                  WHERE user_id IN (SELECT user_id FROM loadtest_users);
DELETE FROM ai_reports            WHERE user_id IN (SELECT user_id FROM loadtest_users);
DELETE FROM user_profiles         WHERE user_id IN (SELECT user_id FROM loadtest_users);
DELETE FROM local_accounts        WHERE user_id IN (SELECT user_id FROM loadtest_users);
DELETE FROM users                 WHERE id      IN (SELECT user_id FROM loadtest_users);

SELECT count(*) AS deleted_users FROM loadtest_users;

COMMIT;
