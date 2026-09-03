# Flyway 스키마 마이그레이션

dev/prod의 스키마는 Flyway가 만듭니다. Hibernate는 `validate`로 대조만 합니다.

Flyway는 스키마의 최종 형태를 선언하는 방식이 아니라, 변경분을 담은 SQL 파일을 버전 순서대로 쌓아
적용하는 방식입니다. 어디까지 적용했는지는 DB의 `flyway_schema_history` 테이블이 기억하므로 기동할
때마다 아직 적용되지 않은 파일만 실행됩니다. 이미 적용된 파일은 체크섬으로 대조해, 내용이 바뀌었으면
기동이 실패합니다.

## 규칙

- 파일은 `src/main/resources/db/migration`에 `V{n}__설명.sql`로 둡니다.
  - `n`은 기존 최댓값 + 1, 설명은 snake_case입니다.
- **적용된 파일은 절대 수정하면 안 됩니다.**
  - 스키마, 엔티티의 변경이 발생한 경우 새 버전 파일로 추가해주세요.
- dev에는 Flyway 도입 전 스키마가 남아 있어 `baseline-on-migrate`로 그것을 V1 적용 상태로 기록합니다. baseline이 기록된 뒤 제거합니다.

## 사용 방법

엔티티를 바꾸면 같은 커밋에 마이그레이션 파일을 추가합니다. 식별자는 V1과 같이 큰따옴표로 감쌉니다.

```sql
-- 테이블 추가
CREATE TABLE "<테이블>" ( ... );
-- 컬럼 추가·삭제
ALTER TABLE "<테이블>" ADD COLUMN "<컬럼>" VARCHAR(50) NOT NULL;
ALTER TABLE "<테이블>" DROP COLUMN "<컬럼>";
-- 컬럼 타입·이름 변경
ALTER TABLE "<테이블>" ALTER COLUMN "<컬럼>" TYPE VARCHAR(100);
ALTER TABLE "<테이블>" RENAME COLUMN "<이전>" TO "<이후>";

-- 타입을 바꿀 때 기존 값이 새 타입으로 자동 변환되지 않으면 `USING`으로 변환식을 지정합니다.
ALTER TABLE "<테이블>" ALTER COLUMN "<컬럼>" TYPE INTEGER USING "<컬럼>"::INTEGER;
```

### 주의 사항

1. 데이터가 있는 테이블에 `NOT NULL` 컬럼을 추가하면 기존 행을 채울 값이 없어 실패합니다. `DEFAULT`를 함께
주거나, 컬럼 추가 → 값 채우기 → `SET NOT NULL` 세 문장으로 나눕니다.
2. 타입을 바꿀 때 기존 값이 새 타입으로 자동 변환되지 않으면 `USING`으로 변환식을 지정합니다. 변환할 수 없는
행이 하나라도 있으면 마이그레이션 전체가 실패합니다.
3. **컬럼 삭제는 되돌릴 수 없습니다.** 애플리케이션에서 참조를 먼저 없애 배포한 뒤, 다음 마이그레이션에서
지웁니다. 이름 변경은 `RENAME COLUMN`이 데이터를 보존하므로 엔티티 필드명과 같은 커밋에서 바꿉니다.

### 버전 번호 충돌

**머지 전에 `develop`의 최신 번호를 다시
확인하고, 아직 어디에도 적용되지 않은 파일이면 번호를 올립니다.** 이미 dev에 적용된 파일은 번호도 내용도
바꾸지 않습니다.

## 테스트 절차

1. `migration/FlywaySchemaValidationTest`가 Flyway 적용 → `validate` 순서를 재현합니다.
2. CI의 `test` job이 PostgreSQL과 `SCHEMA_CHECK_DB_*`를 주입해 PR마다 실행합니다. 마이그레이션 누락은 dev 배포가 아니라
CI에서 걸립니다.
3. `validate`는 테이블·컬럼의 존재와 타입만 보고 인덱스·외래 키·기본값은 보지 않습니다. 마이그레이션에만 있고 엔티티에는 없는 컬럼은 통과합니다.

## 체크리스트

- 번호가 `develop`의 최신 마이그레이션보다 큰가
- 이미 적용된 파일을 고치지 않았는가
- 엔티티 변경과 같은 커밋에 있는가
- 데이터가 사라지는 변경이면 단계를 나눴는가

## 비고

- 기동 시 마이그레이션은 `spring-boot-starter-flyway`가 수행합니다.
  - 이 사항은 flyway의 공식 문서에서는 언급되어 있지 않습니다. spring-boot 4의 호환성 문제입니다.
