# ArchUnit 컨벤션

아키텍처 규칙은 문서로만 남기지 않고 ArchUnit 단위 테스트로 강제합니다. (`com.tngtech.archunit:archunit-junit5`)

## 위치와 작성 방법

- 전 모듈 공통 아키텍처 테스트는 `src/test/java/com/daesabu/meongcoach/architecture/`에 둡니다.
- `@AnalyzeClasses(packages = "com.daesabu.meongcoach")` + `@ArchTest` 필드 방식으로 작성합니다.

## 검증 항목

다음 다섯 범주를 검증합니다. 새 아키텍처 규칙이 생기면 해당 범주의 테스트에 추가합니다.

### 1. 계층형 아키텍처 의존관계

모듈 내부 의존 방향 `adapter → application → domain`을 검증합니다.

```java
@ArchTest
static final ArchRule 계층_의존_방향 = layeredArchitecture()
	.consideringOnlyDependenciesInLayers()
	.layer("Adapter").definedBy("..adapter..")
	.layer("Application").definedBy("..application..")
	.layer("Domain").definedBy("..domain..")
	.whereLayer("Adapter").mayNotBeAccessedByAnyLayer()
	.whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter")
	.whereLayer("Domain").mayOnlyBeAccessedByLayers("Adapter", "Application");
```

### 2. 순환 의존성

패키지·클래스 간 순환 의존성을 금지합니다.

```java
@ArchTest
static final ArchRule 모듈_순환_금지 = slices()
	.matching("com.daesabu.meongcoach.(*)..")
	.should().beFreeOfCycles();
```

### 3. 이름 형식

클래스 이름이 [code-convention.md](code-convention.md)의 역할별 네이밍을 따르는지 검증합니다.

```java
@ArchTest
static final ArchRule 컨트롤러_네이밍 = classes()
	.that().areAnnotatedWith(RestController.class)
	.should().haveSimpleNameEndingWith("Controller");
```

### 4. 애노테이션 적용 패턴

애노테이션이 허용된 계층에만 사용되는지 검증합니다.

```java
@ArchTest
static final ArchRule 컨트롤러_위치 = classes()
	.that().areAnnotatedWith(RestController.class)
	.should().resideInAPackage("..adapter.webapi..");

@ArchTest
static final ArchRule 도메인_스프링_금지 = noClasses()
	.that().resideInAPackage("..domain..")
	.should().dependOnClassesThat().resideInAPackage("org.springframework..");
```

### 5. 의존관계와 사용관계 구분

- **의존관계**(`dependOnClassesThat`): 필드, 파라미터, 리턴 타입, 상속 등 선언 수준까지 포함한 넓은 검증. 계층 격리 규칙에 사용합니다.
- **사용관계**(`accessClassesThat`): 메서드 호출·필드 접근 등 실제 실행 코드 수준의 좁은 검증. 특정 API 호출 금지 규칙에 사용합니다.

규칙을 작성할 때 무엇을 막으려는지에 따라 두 API를 구분해서 사용합니다.

```java
// 의존 자체를 금지 (타입 참조도 불가)
noClasses().that().resideInAPackage("..application..")
	.should().dependOnClassesThat().resideInAPackage("..adapter..");

// 직접 호출만 금지 (타입 참조는 허용)
noClasses().should().accessClassesThat().belongToAnyOf(LocalDateTime.class);
```
