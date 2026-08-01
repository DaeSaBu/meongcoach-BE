# 온보딩 API 설계

## 완료 요청 계약

`POST /api/onboarding`은 사용자 프로필, 교육 선택과 반려견 프로필을 한 번에 받는다.
인증된 회원 ID는 `@CurrentUserId`로만 식별하고 요청 본문에서 받지 않는다.

### 교육·기대 사항 필드

| 필드 | JSON 타입 | null·빈 값 처리 | 검증 | 저장 위치 |
|---|---|---|---|---|
| `priorTrainingTopicIds` | 정수 배열 | 미전송·`null`·빈 배열은 선택 없음 | 최대 100개, 원소는 null이 아닌 양수이자 존재하는 토픽 ID | `user_profiles.prior_training_topic_ids` (`BIGINT[]`) |
| `trainingGoalTopicIds` | 정수 배열 | 미전송·`null`·빈 배열은 선택 없음 | 최대 100개, 원소는 null이 아닌 양수이자 존재하는 토픽 ID | `user_profiles.training_goal_topic_ids` (`BIGINT[]`) |
| `dogs[].expectation` | 문자열 | 미전송·`null`은 빈 문자열로 저장 | 최대 500자 | `dogs.expectation` |

- 각 토픽 배열 안의 중복 ID는 한 번만 저장한다.
- 같은 토픽이 교육 이력과 교육 목표에 동시에 포함되는 것은 허용한다. 이미 경험했지만 계속 교육하려는 경우를
  표현할 수 있고, 두 선택은 서로 독립된 배열 컬럼에 저장된다.
- 배열의 원소가 `null` 또는 0 이하이면 Bean Validation에 따라 400 `BAD_REQUEST`를 반환한다.
- 양수지만 존재하지 않는 토픽 ID가 하나라도 있으면 404 `TRAINING_TOPIC_NOT_FOUND`를 반환한다.

## 저장 구조

```text
user_profiles
├─ user_id (PK, FK -> users.id)
├─ prior_training_topic_ids BIGINT[] NOT NULL DEFAULT '{}'
└─ training_goal_topic_ids BIGINT[] NOT NULL DEFAULT '{}'

dogs
└─ expectation TEXT NOT NULL DEFAULT ''
```

교육 토픽은 요청 최상위 필드이므로 현재는 사용자 단위 선택으로 저장한다. `expectation`은 반려견마다 다른 값이므로
`dogs` 행에 저장한다. 토픽 ID는 PostgreSQL `BIGINT[]`로 보관하며 배열 원소에는 데이터베이스 외래 키를 설정할 수 없다.
따라서 user 모듈에서 토픽 엔티티를 직접 참조하지 않고, 저장 전 training 모듈의 `TopicValidator` 공개 인터페이스로
존재 여부를 검증한다.

운영 PostgreSQL에는 애플리케이션 배포 전에
[`docs/database/dae-206-onboarding-training-settings.sql`](database/dae-206-onboarding-training-settings.sql)을 적용한다.
운영 프로파일은 `ddl-auto: validate`라 스키마를 먼저 반영하지 않으면 변경된 애플리케이션이 기동하지 않는다.

## 처리 흐름

```text
OnboardingController
  → 요청 형식·길이·배열 원소 검증
  → StoredImageUrlValidator로 이미지 URL 검증
  → TopicValidator로 교육 이력·목표 토픽 존재 검증
  → UserProfileRegister로 프로필·교육 선택 저장
  → DogRegister로 반려견별 기대 사항 저장
  → 생성된 dogIds 반환
```

프로필, 두 토픽 배열과 모든 반려견은 기존 온보딩 쓰기 트랜잭션 안에서 함께 저장된다. 어느 단계에서든 실패하면
전체 변경을 롤백하며 부분 온보딩 상태를 남기지 않는다.

## 조회 및 연동 영향

- 현재 온보딩 완료 응답은 기존과 동일하게 `dogIds`만 반환한다. 신규 입력값을 응답에 되돌려 주지 않는다.
- 현재 사용자·반려견 프로필 조회 API가 없으므로 기존 조회 응답의 변경은 없다. 이후 프로필 조회 API를 만들 때
  `priorTrainingTopicIds`, `trainingGoalTopicIds`, 반려견별 `expectation` 노출 여부를 함께 결정해야 한다.
- 토픽 이름이 필요한 조회에서는 저장된 ID를 training 모듈의 공개 API로 일괄 조회해야 한다. user 모듈에서
  training 도메인이나 리포지토리를 직접 참조하지 않는다.
- 배열 원소에는 외래 키가 없으므로 토픽 삭제 기능을 추가할 때 사용자 프로필에 남은 ID의 정리 정책을 함께 정해야 한다.
- 특정 토픽을 선택한 사용자 검색이 필요해지면 실제 조회 패턴을 확인한 뒤 배열 포함 연산과 GIN 인덱스를 검토한다.
- 토픽 선택은 현재 사용자 단위다. 반려견별 교육 이력·목표가 필요해지면 요청을 `dogs[]` 아래로 이동하고
  dog-topic 관계 테이블로 분리하는 별도 마이그레이션이 필요하다.
