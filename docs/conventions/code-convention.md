# 코드 컨벤션

## 코드 스타일

스타일의 원천은 저장소 루트의 `.editorconfig`와 `.idea/codeStyles/`입니다. (Wooteco 코드 스타일 기반)
이 문서에 스타일 규칙을 중복 기술하지 않으며, IDE가 자동 적용하는 설정을 그대로 따릅니다.

핵심만 요약하면:

- 들여쓰기: 탭 (크기 4)
- 한 줄 최대 120자
- `if` / `for` / `while` 등에 중괄호 항상 사용
- 와일드카드 import 금지

## 네이밍

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| 패키지(모듈) | 소문자, 단수형 | `user`, `walk`, `shared` |
| 클래스 | PascalCase | `UserQueryService` |
| 메서드/변수 | camelCase | `findByName` |
| 상수 | UPPER_SNAKE_CASE | `MAX_WALK_TIME` |

### 아키텍처 역할별 네이밍

| 역할 | 위치 | 규칙 | 예시 |
| --- | --- | --- | --- |
| 모듈 공개 API 인터페이스 | `application/provided` | 능력을 나타내는 이름, 접미사 없음 | `UserRegister`, `UserFinder` |
| 애플리케이션 조회 결과 모델 | `application/provided` | `~View` (record) | `CurriculumView`, `LessonView` |
| 필요 자원 인터페이스 | `application/required` | 자원 이름 그대로 | `UserRepository`, `EmailSender` |
| 애플리케이션 서비스 | `application` | `~Service` | `UserQueryService`, `UserFinderService` |
| 컨트롤러 | `adapter/webapi` | `~Controller` | `UserController` |
| 도메인 모델 | `domain` | 개념 이름 그대로 | `User` |
| 도메인 입력 모델 | `domain` | `~Command` (record) | `DogRegisterCommand` |
| 값 객체 | `domain/vo` | 개념 이름 그대로 | `Email` |
| 도메인 예외·에러코드 | `domain/exception` | `{모듈}ErrorCode`, `~Exception` | `UserErrorCode`, `InvalidEmailException` |

- `domain` 루트에는 엔티티와 enum을 두고, 값 객체는 `domain/vo`, 예외·에러코드는 `domain/exception`으로 분리합니다.

## Lombok 사용 규칙

- 허용: `@Getter`, `@RequiredArgsConstructor`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`(JPA 엔티티), `@Slf4j`(로깅)
- 지양: `@Setter`, `@Data`, `@AllArgsConstructor`, `@Builder`, `@ToString`(엔티티 — 연관관계 순환 참조 위험)
- 도메인 객체의 상태 변경은 setter가 아닌 의도가 드러나는 메서드로 표현합니다.

## DTO

- 요청/응답 DTO는 Java `record`로 작성합니다.
- 웹 요청/응답 DTO는 `adapter/webapi/dto`에 두고, 접미사는 요청 `~Request`, 응답 `~Response`를 사용합니다. (예: `UserSignupRequest`, `UserResponse`)
- 도메인 입력 모델은 `~Command` 접미사의 record로 `domain`에 두며, 웹 DTO와 별개로 유지합니다. (예: `DogRegisterCommand`)
	- 엔티티 정적 팩토리의 순수 값 파라미터가 3개 이상이면 Command로 묶고, 팩토리는 Command를 받아 생성자에 전달합니다.
	- 연관 엔티티는 Command에 담지 않고 별도 인자로 전달합니다. (예: `Curriculum.create(Topic topic, CurriculumCreateCommand command)`)
- 애플리케이션 조회 결과는 `application/provided`의 `~View` record로 반환합니다. 필드명은 도메인 기준(`id`, `title`, `sortOrder`)으로 두고, 웹 노출 이름(`topicId`, `topicTitle`)으로 바꾸는 일은 `~Response.from(~View)` 정적 팩토리에서만 합니다. `~View`에 웹 네이밍을 쓰면 `application`이 `adapter`의 관심사를 떠안게 됩니다.
- DTO ↔ 도메인 변환은 DTO의 정적 팩토리 메서드(`from`, `of`) 또는 `toXxx` 메서드로 처리합니다.
- 엔티티를 컨트롤러 응답으로 직접 노출하지 않습니다.
