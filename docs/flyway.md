# Flyway 스키마 마이그레이션

dev/prod의 스키마는 Flyway가 만들고 Hibernate는 `validate`로 대조만 합니다. local/test는 ddl-auto가
스키마를 만들어 Flyway를 끕니다. 프로파일 구성은 [profiles.md](profiles.md)를 참고하세요.

## 규칙

- 파일은 `src/main/resources/db/migration`에 `V{n}__설명.sql`로 둡니다. `n`은 기존 최댓값 + 1, 설명은 snake_case입니다.
- 적용된 파일은 수정하지 않습니다. 체크섬이 어긋나면 기동이 실패하므로 스키마 변경은 새 버전 파일로 추가합니다.
- 엔티티를 바꾸면 마이그레이션도 함께 추가합니다. local/test는 ddl-auto가 스키마를 대신 만들어 빠뜨려도 동작하므로 리뷰에서 확인합니다.
- 기동 시 마이그레이션은 `spring-boot-starter-flyway`의 자동 구성이 수행합니다. Spring Boot 4가 이를 별도 모듈로 분리했으므로, 의존성이 없으면 `spring.flyway` 설정이 있어도 오류 없이 실행되지 않습니다.
- dev에는 Flyway 도입 전 스키마가 남아 있어 `baseline-on-migrate`로 그것을 V1 적용 상태로 기록합니다. baseline이 기록된 뒤 제거합니다.

## 검증

`migration/FlywaySchemaValidationTest`가 Flyway 적용 → `validate` 순서를 재현합니다. CI의 `test` job이
PostgreSQL과 `SCHEMA_CHECK_DB_*`를 주입해 PR마다 실행하므로, 마이그레이션 누락은 dev 배포가 아니라
CI에서 걸립니다. `validate`는 테이블·컬럼의 존재와 타입만 보고 인덱스·외래 키·기본값은 보지 않으며,
마이그레이션에만 있고 엔티티에는 없는 컬럼은 통과합니다.
