# ArchUnit 컨벤션

아키텍처 규칙은 문서로만 남기지 않고 ArchUnit 단위 테스트로 강제합니다. (`com.tngtech.archunit:archunit-junit5`)

## 위치와 작성 방법

- 전 모듈 공통 아키텍처 테스트는 `src/test/java/com/daesabu/meongcoach/architecture/`에 둡니다.
- `ClassFileImporter`로 임포트한 `JavaClasses` 상수를 두고, 일반 `@Test` 메서드에서 `ArchRule.check()`로 검증합니다.
- 메서드명은 영어 camelCase로 쓰고, 한글 설명은 `@DisplayName`으로 붙입니다. (`@ArchTest` 필드에는 `@DisplayName`을 붙일 수 없으므로 필드 방식을 쓰지 않습니다)

```java
private static final JavaClasses CLASSES = new ClassFileImporter()
		.withImportOption(new ImportOption.DoNotIncludeTests())
		.importPackages("com.daesabu.meongcoach");
```

## 검증 항목

다음 다섯 범주를 검증합니다. 새 아키텍처 규칙이 생기면 해당 범주의 테스트에 추가합니다.

### 1. 계층형 아키텍처 의존관계

모듈 내부 의존 방향 `adapter → application → domain`을 검증합니다.

```java
@Test
@DisplayName("모듈 내부 의존은 adapter → application → domain 방향을 따른다")
void layersFollowDependencyDirection() {
	layeredArchitecture()
		.consideringOnlyDependenciesInLayers()
		.layer("Adapter").definedBy("..adapter..")
		.layer("Application").definedBy("..application..")
		.layer("Domain").definedBy("..domain..")
		.whereLayer("Adapter").mayNotBeAccessedByAnyLayer()
		.whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter")
		.whereLayer("Domain").mayOnlyBeAccessedByLayers("Adapter", "Application")
		.check(CLASSES);
}
```

### 2. 순환 의존성

패키지·클래스 간 순환 의존성을 금지합니다.

```java
@Test
@DisplayName("모듈 간 순환 의존이 없다")
void modulesAreFreeOfCycles() {
	slices()
		.matching("com.daesabu.meongcoach.(*)..")
		.should().beFreeOfCycles()
		.check(CLASSES);
}
```

### 3. 이름 형식

클래스 이름이 [code-convention 스킬](../../.claude/skills/code-convention/SKILL.md)의 역할별 네이밍을 따르는지 검증합니다.

```java
@Test
@DisplayName("RestController는 Controller 접미사를 가진다")
void controllersEndWithControllerSuffix() {
	classes()
		.that().areAnnotatedWith(RestController.class)
		.should().haveSimpleNameEndingWith("Controller")
		.check(CLASSES);
}

// domain 루트의 입력 모델 record는 ~Command 네이밍을 따른다 (값 객체가 있는 domain/vo는 제외)
@Test
@DisplayName("domain 루트의 입력 모델 record는 Command 접미사를 가진다")
void domainInputModelRecordsEndWithCommand() {
	classes()
		.that().resideInAPackage("..domain")
		.and().areRecords()
		.should().haveSimpleNameEndingWith("Command")
		.check(CLASSES);
}
```

### 4. 애노테이션 적용 패턴

애노테이션이 허용된 계층에만 사용되는지 검증합니다.

```java
@Test
@DisplayName("RestController는 adapter/webapi에만 둔다")
void controllersResideInAdapterWebapi() {
	classes()
		.that().areAnnotatedWith(RestController.class)
		.should().resideInAPackage("..adapter.webapi..")
		.check(CLASSES);
}

@Test
@DisplayName("domain은 스프링에 의존하지 않는다")
void domainDoesNotDependOnSpring() {
	noClasses()
		.that().resideInAPackage("..domain..")
		.should().dependOnClassesThat().resideInAPackage("org.springframework..")
		.check(CLASSES);
}
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
