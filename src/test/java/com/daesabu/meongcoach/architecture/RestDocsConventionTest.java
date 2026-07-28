package com.daesabu.meongcoach.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RestDocs 컨벤션 검증. (docs/conventions/restdocs-convention.md)
 *
 * <p>스니펫 식별자와 필드 설명은 모두 문자열 리터럴이라 바이트코드에 남지 않는다. ArchUnit으로는 볼 수 없으므로 테스트 소스를 직접 읽어 검사한다.
 * `.optional()`·`.type()` 누락처럼 응답 값에 따라 달라지는 규칙은 정적으로 판단할 수 없어 리뷰에서 확인한다.
 */
@DisplayName("RestDocs 컨벤션 검증")
class RestDocsConventionTest {

	private static final Path TEST_SOURCE_ROOT = Path.of("src", "test", "java");
	private static final Path MODULE_ROOT = Path.of("src", "main", "java", "com", "daesabu", "meongcoach");
	private static final Path INDEX_ADOC = Path.of("src", "docs", "asciidoc", "index.adoc");

	// 자기 자신은 document( 리터럴을 패턴으로 갖고 있어 검사 대상에서 제외한다
	private static final String SELF = RestDocsConventionTest.class.getSimpleName() + ".java";

	private static final String DOCUMENT_STATIC_IMPORT =
			"import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;";
	private static final String QUALIFIED_DOCUMENT_CALL = "MockMvcRestDocumentation.document(";
	private static final String MOCK_MVC_REQUEST_BUILDERS =
			"org.springframework.test.web.servlet.request.MockMvcRequestBuilders";

	// document("user/register", ...) 의 첫 인자
	private static final Pattern SNIPPET_IDENTIFIER = Pattern.compile("\\bdocument\\(\\s*\"([^\"]*)\"");
	// {모듈}/{행위} 소문자 kebab-case
	private static final Pattern IDENTIFIER_FORMAT =
			Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*/[a-z0-9]+(?:-[a-z0-9]+)*");
	// 경로 변수를 포함한 URI 템플릿 리터럴 ("/api/dogs/{dogId}")
	private static final Pattern URI_WITH_PATH_VARIABLE = Pattern.compile("\"/[^\"]*\\{[^\"]*\"");
	// description("") 처럼 비어 있는 설명
	private static final Pattern BLANK_DESCRIPTION = Pattern.compile("description\\(\\s*\"\\s*\"\\s*\\)");
	// index.adoc 의 operation::user/register[...] (// 로 시작하는 주석 줄은 매칭되지 않는다)
	private static final Pattern OPERATION_MACRO = Pattern.compile("(?m)^operation::([^\\[\\s]+)\\[");

	private static final List<DocumentedTest> DOCUMENTED_TESTS = loadDocumentedTests();
	private static final Set<String> MODULE_NAMES = loadModuleNames();

	@Test
	@DisplayName("문서화 테스트를 하나 이상 수집한다")
	void documentedTestsAreCollected() {
		assertThat(DOCUMENTED_TESTS)
				.withFailMessage("문서화 테스트를 찾지 못했다. 작업 디렉터리가 프로젝트 루트인지 확인한다.")
				.isNotEmpty();
	}

	@Test
	@DisplayName("문서화 테스트는 @WebMvcTest와 @AutoConfigureRestDocs를 함께 선언한다")
	void documentedTestsDeclareRestDocsAnnotations() {
		List<String> violations = new ArrayList<>();
		for (DocumentedTest test : DOCUMENTED_TESTS) {
			if (!test.source().contains("@WebMvcTest")) {
				violations.add(test.fileName() + " — @WebMvcTest 누락");
			}
			if (!test.source().contains("@AutoConfigureRestDocs")) {
				violations.add(test.fileName() + " — @AutoConfigureRestDocs 누락");
			}
		}

		assertNoViolation(violations, "문서화 테스트에 RestDocs 애노테이션이 없다");
	}

	@Test
	@DisplayName("document(...)는 정적 임포트해 클래스명 없이 호출한다")
	void documentIsCalledThroughStaticImport() {
		List<String> violations = new ArrayList<>();
		for (DocumentedTest test : DOCUMENTED_TESTS) {
			if (test.source().contains(QUALIFIED_DOCUMENT_CALL)) {
				violations.add(test.fileName() + " — " + QUALIFIED_DOCUMENT_CALL + " 형태로 호출했다");
			}
			if (!test.source().contains(DOCUMENT_STATIC_IMPORT)) {
				violations.add(test.fileName() + " — document 정적 임포트 누락");
			}
		}

		assertNoViolation(violations, "document(...) 호출부를 한 줄 임포트 교체로 바꿀 수 없다");
	}

	@Test
	@DisplayName("요청은 RestDocumentationRequestBuilders로 만든다")
	void requestsAreBuiltWithRestDocumentationRequestBuilders() {
		List<String> violations = DOCUMENTED_TESTS.stream()
				.filter(test -> test.source().contains(MOCK_MVC_REQUEST_BUILDERS))
				.map(test -> test.fileName() + " — MockMvcRequestBuilders 사용. 경로 변수가 문서에 남지 않는다")
				.toList();

		assertNoViolation(violations, "요청 빌더가 RestDocumentationRequestBuilders가 아니다");
	}

	@Test
	@DisplayName("스니펫 식별자는 {모듈}/{행위} 소문자 kebab-case 형식이다")
	void snippetIdentifiersFollowNamingFormat() {
		List<String> violations = new ArrayList<>();
		for (DocumentedTest test : DOCUMENTED_TESTS) {
			for (String identifier : test.identifiers()) {
				if (!IDENTIFIER_FORMAT.matcher(identifier).matches()) {
					violations.add(test.fileName() + " — \"" + identifier + "\"");
				}
			}
		}

		assertNoViolation(violations, "스니펫 식별자 형식이 {모듈}/{행위}가 아니다");
	}

	@Test
	@DisplayName("스니펫 식별자의 모듈 세그먼트는 실제 모듈 패키지명이다")
	void snippetIdentifierModulesMatchPackageNames() {
		List<String> violations = new ArrayList<>();
		for (DocumentedTest test : DOCUMENTED_TESTS) {
			for (String identifier : test.identifiers()) {
				String module = identifier.split("/", 2)[0];
				if (!MODULE_NAMES.contains(module)) {
					violations.add(test.fileName() + " — \"" + identifier + "\"의 모듈 " + module);
				}
			}
		}

		assertNoViolation(violations, "모듈 세그먼트가 " + MODULE_NAMES + " 중 하나가 아니다");
	}

	@Test
	@DisplayName("스니펫 식별자는 저장소 전체에서 유일하다")
	void snippetIdentifiersAreUnique() {
		Map<String, List<String>> owners = new LinkedHashMap<>();
		for (DocumentedTest test : DOCUMENTED_TESTS) {
			for (String identifier : test.identifiers()) {
				owners.computeIfAbsent(identifier, key -> new ArrayList<>()).add(test.fileName());
			}
		}

		List<String> violations = owners.entrySet().stream()
				.filter(entry -> entry.getValue().size() > 1)
				.map(entry -> "\"" + entry.getKey() + "\" — " + entry.getValue())
				.toList();

		assertNoViolation(violations, "같은 식별자를 여러 곳에서 사용해 스니펫이 서로 덮어써진다");
	}

	@Test
	@DisplayName("경로 변수를 쓰는 문서화 테스트는 pathParameters로 문서화한다")
	void pathVariablesAreDocumented() {
		List<String> violations = DOCUMENTED_TESTS.stream()
				.filter(test -> URI_WITH_PATH_VARIABLE.matcher(test.source()).find())
				.filter(test -> !test.source().contains("pathParameters("))
				.map(test -> test.fileName() + " — pathParameters 누락")
				.toList();

		assertNoViolation(violations, "경로 변수를 사용하면서 문서화하지 않았다");
	}

	@Test
	@DisplayName("필드 설명을 비워 두지 않는다")
	void fieldDescriptionsAreNotBlank() {
		List<String> violations = DOCUMENTED_TESTS.stream()
				.filter(test -> BLANK_DESCRIPTION.matcher(test.source()).find())
				.map(test -> test.fileName() + " — 빈 description")
				.toList();

		assertNoViolation(violations, "설명이 비어 있는 필드가 있다");
	}

	@Test
	@DisplayName("문서화한 스니펫과 index.adoc의 operation 매크로가 일치한다")
	void documentedSnippetsAreIncludedInIndexAdoc() {
		Set<String> documented = DOCUMENTED_TESTS.stream()
				.flatMap(test -> test.identifiers().stream())
				.collect(Collectors.toCollection(LinkedHashSet::new));
		Set<String> included = OPERATION_MACRO.matcher(read(INDEX_ADOC)).results()
				.map(result -> result.group(1))
				.collect(Collectors.toCollection(LinkedHashSet::new));

		List<String> violations = new ArrayList<>();
		documented.stream()
				.filter(identifier -> !included.contains(identifier))
				.forEach(identifier -> violations.add("\"" + identifier + "\" — index.adoc에 섹션이 없다"));
		included.stream()
				.filter(identifier -> !documented.contains(identifier))
				.forEach(identifier -> violations.add("\"" + identifier + "\" — 생성되지 않는 스니펫을 참조한다"));

		assertNoViolation(violations, "테스트 스니펫과 index.adoc이 어긋난다");
	}

	private static void assertNoViolation(List<String> violations, String rule) {
		assertThat(violations)
				.withFailMessage("%s (docs/conventions/restdocs-convention.md)%n%s",
						rule, String.join(System.lineSeparator(), violations))
				.isEmpty();
	}

	private static List<DocumentedTest> loadDocumentedTests() {
		try (Stream<Path> paths = Files.walk(TEST_SOURCE_ROOT)) {
			return paths.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().endsWith(".java"))
					.filter(path -> !path.getFileName().toString().equals(SELF))
					.map(RestDocsConventionTest::toDocumentedTest)
					.filter(test -> !test.identifiers().isEmpty())
					.toList();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static DocumentedTest toDocumentedTest(Path path) {
		String source = read(path);
		List<String> identifiers = SNIPPET_IDENTIFIER.matcher(source).results()
				.map(result -> result.group(1))
				.toList();
		return new DocumentedTest(path.getFileName().toString(), source, identifiers);
	}

	private static Set<String> loadModuleNames() {
		try (Stream<Path> paths = Files.list(MODULE_ROOT)) {
			return paths.filter(Files::isDirectory)
					.map(path -> path.getFileName().toString())
					.collect(Collectors.toCollection(LinkedHashSet::new));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String read(Path path) {
		try {
			return Files.readString(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private record DocumentedTest(String fileName, String source, List<String> identifiers) {
	}
}
