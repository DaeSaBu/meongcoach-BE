# Flyway 스키마 마이그레이션

스키마는 Flyway가 관리합니다. 마이그레이션 파일은 `src/main/resources/db/migration`에 두며,
초기 스키마는 `V1__init_schema.sql`입니다. 프로파일 자체의 구성은 [profiles.md](profiles.md)를 참고하세요.

## ddl-auto 정책

| 프로파일 | 값 | 함의 |
|---|---|---|
| `local` | `create-drop` | 애플리케이션 기동 시 엔티티 스키마와 교육 초기 데이터·테스트 계정을 재생성. Flyway는 `spring.flyway.enabled: false`로 꺼 둔다 |
| `test` | `create-drop` | H2 인메모리 스키마를 테스트마다 재생성. H2는 마이그레이션(PostgreSQL 문법)을 실행할 수 없어 Flyway를 끈다 |
| `dev`, `prod` | `validate` | 스키마를 자동 변경하지 않음. Flyway 마이그레이션이 스키마를 만들고, 엔티티와 불일치하면 기동 실패 |

- dev/prod는 애플리케이션 기동 시 `spring.flyway`가 자동으로 실행됩니다. 별도 접속 정보 없이 이미 구성된 DataSource(`DB_HOST`/`DB_NAME`/`DB_USERNAME`/`DB_PASSWORD`)를 그대로 재사용합니다.
- 기동 시 마이그레이션은 `spring-boot-starter-flyway`가 제공하는 자동 구성이 수행합니다. Spring Boot 4는 Flyway 자동 구성을 별도 모듈로 분리했으므로, 이 의존성이 없으면 `spring.flyway` 설정이 있어도 오류 없이 마이그레이션이 실행되지 않습니다.
- dev에는 Flyway 도입 전 `ddl-auto`가 만든 스키마가 남아 있어 `baseline-on-migrate: true`, `baseline-version: 1`로 그 스키마를 V1 적용 상태로 기록합니다. `flyway_schema_history`에 baseline이 기록된 뒤에는 제거합니다. 남겨 두면 마이그레이션 없이 만들어진 스키마를 다시 baseline으로 삼을 수 있습니다.

### 동작 원리

- 마이그레이션 파일은 버전(`V1`, `V2`, ...) 순서로 실행되며, DB의 `flyway_schema_history` 테이블에 적용 이력을 기록합니다.
- 애플리케이션 기동 시 이 테이블을 기준으로 아직 적용되지 않은 버전만 순서대로 실행합니다.
- 이미 적용된 파일은 체크섬으로 무결성을 검증합니다. 적용 후 파일 내용이 바뀌면 체크섬이 어긋나 기동이 실패합니다.

### 새 마이그레이션 추가

- 파일명은 `V{n}__설명.sql`이며, `n`은 기존 마이그레이션 파일의 최댓값 + 1, 설명은 snake_case로 씁니다.
- 이미 적용된 마이그레이션 파일은 수정하지 않고, 스키마를 더 바꿔야 하면 새 버전 파일을 추가합니다.
- 엔티티를 변경하면 마이그레이션 파일을 함께 추가합니다. dev/prod는 `validate`라 어긋나면 기동이 실패하지만, local/test는 ddl-auto가 스키마를 대신 만들어 마이그레이션 없이도 동작하므로 리뷰에서 엔티티 변경과 마이그레이션 동반 여부를 확인합니다.
- 작성한 마이그레이션은 `./gradlew flywayMigrate`로 로컬 postgres(`compose.yml`)의 검증 전용 DB `meongcoach_schema_check`에 먼저 적용해 검증합니다. 앱이 쓰는 `meongcoach`와 분리돼 있어 `create-drop`이 만든 테이블과 Flyway 이력이 섞이지 않습니다.
- `migration/FlywaySchemaValidationTest`가 Flyway 적용 → `ddl-auto: validate` 순서로 엔티티와 스키마를 대조합니다. `SCHEMA_CHECK_DB_*` 환경 변수가 있을 때만 켜지고 없으면 건너뛰므로 평소 테스트 실행에는 영향이 없습니다. CI의 `test` job은 이 변수를 주입해 PR마다 검증하며, 마이그레이션을 빠뜨리면 dev 배포가 아니라 CI에서 실패합니다.
- `validate`는 테이블·컬럼의 존재와 타입만 확인합니다. 인덱스, 외래 키, 기본값, 컬럼 순서는 확인 대상이 아닙니다. 엔티티를 기준으로 한 방향으로만 대조하므로 마이그레이션에만 있고 엔티티에는 없는 컬럼·테이블은 통과합니다.
- 로컬에서 같은 검증을 돌리려면 compose postgres를 띄운 뒤 다음을 실행합니다.
  ```bash
  SCHEMA_CHECK_DB_URL=jdbc:postgresql://localhost:5432/meongcoach_schema_check \
  SCHEMA_CHECK_DB_USERNAME=meongcoach_local \
  SCHEMA_CHECK_DB_PASSWORD=meongcoach-local \
  ./gradlew test --tests 'com.daesabu.meongcoach.migration.FlywaySchemaValidationTest'
  ```
