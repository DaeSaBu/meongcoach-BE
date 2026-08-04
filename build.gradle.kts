import groovy.json.JsonOutput
import groovy.json.JsonSlurper

plugins {
	java
	jacoco
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.asciidoctor.jvm.convert") version "4.0.4"
	// 0.20.x가 Spring Boot 4.x / REST Docs 4.x 지원 라인이다
	id("com.epages.restdocs-api-spec") version "0.20.1"
	id("com.diffplug.spotless") version "8.8.0"
}
val springAiVersion by extra("2.0.0")

group = "com.daesabu"
version = "0.0.1-SNAPSHOT"
description = "meongcoach"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

val asciidoctorExt: Configuration by configurations.creating

dependencyManagement {
	imports {
		mavenBom("org.springframework.modulith:spring-modulith-bom:2.1.0")
		mavenBom("org.springframework.ai:spring-ai-bom:$springAiVersion")
		// Spring Cloud AWS 4.x가 Spring Boot 4.x 지원 라인이다
		mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:4.1.0")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-h2console")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.modulith:spring-modulith-starter-core")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-restclient")
	// R2는 S3 호환 API라 presigned URL 발급에 AWS SDK의 S3Presigner를 그대로 쓴다
	implementation("software.amazon.awssdk:s3:2.46.7")
	// S3 업로드 완료 이벤트를 SQS로 받아 AI 분석을 트리거한다
	implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
	implementation("org.springframework.ai:spring-ai-starter-model-google-genai")
	// ERROR 로그를 Sentry 이벤트로 전송한다(Logback 연동 내장). Spring Boot 4.x 지원 라인은 -4-starter다
	implementation("io.sentry:sentry-spring-boot-4-starter:8.51.0")
	testImplementation("org.springframework.security:spring-security-test")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("com.h2database:h2")
	runtimeOnly("org.postgresql:postgresql")
	// 로컬 실행 시 compose.yml의 postgres를 자동 기동한다. developmentOnly라 bootJar(배포)에는 포함되지 않는다
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	annotationProcessor("org.projectlombok:lombok")
	asciidoctorExt("org.springframework.restdocs:spring-restdocs-asciidoctor")
	testImplementation("org.springframework.boot:spring-boot-restdocs")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
	testImplementation("com.epages:restdocs-api-spec-mockmvc:0.20.1")
	testImplementation("com.tngtech.archunit:archunit-junit5:1.4.2")
	testImplementation("org.springframework.modulith:spring-modulith-starter-test")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
}

// .editorconfig(Wooteco 스타일) 중 Spotless로 검사 가능한 규칙만 강제한다
// 120자 제한·중괄호 강제 등 파서 수준 규칙은 IDE(.editorconfig)가 담당한다
// 탭 강제는 IntelliJ의 파라미터 스페이스 정렬과 충돌해 Java에는 적용하지 않는다
spotless {
	java {
		target("src/**/*.java")
		importOrder("\\#", "")
		removeUnusedImports()
		trimTrailingWhitespace()
		endWithNewline()
	}
	kotlinGradle {
		target("*.gradle.kts")
		leadingSpacesToTabs()
		trimTrailingWhitespace()
		endWithNewline()
	}
	yaml {
		target("src/**/*.yml", "src/**/*.yaml")
		trimTrailingWhitespace()
		endWithNewline()
	}
}

val snippetsDir = layout.buildDirectory.dir("generated-snippets")

tasks.withType<Test> {
	useJUnitPlatform()
	// 실제 시크릿 없이도 컨텍스트가 뜨도록 테스트 전용 프로파일을 활성화한다
	systemProperty("spring.profiles.active", "test")
}

// 테스트가 끝나면 커버리지 리포트와 RestDocs 문서(build/docs/asciidoc)를 로컬에 생성한다
tasks.test {
	outputs.dir(snippetsDir)
	finalizedBy(tasks.jacocoTestReport, tasks.asciidoctor)
}

// REST Docs 4.0의 asciidoctor 확장은 AsciidoctorJ 3.x를 요구한다
asciidoctorj {
	setVersion("3.0.0")
}

tasks.asciidoctor {
	inputs.dir(snippetsDir)
	configurations("asciidoctorExt")
	dependsOn(tasks.test)
}

// 테스트가 만든 resource.json 스니펫을 OpenAPI 3 스펙(build/api-spec/openapi3.json)으로 합친다
// outputDirectory·outputFileNamePrefix·snippetsDirectory는 기본값이 저장소 구조와 일치해 생략한다
// server는 후처리에서 상대 경로("/")로 덮어쓰므로 여기서 지정하지 않는다
openapi3 {
	title = "멍코치 API"
	description = "멍코치 백엔드 REST API 명세"
	version = project.version.toString()
	format = "json"
}

// 문서화 테스트는 principal()로 인증을 우회해 생성된 스펙에 보안 정보가 없으므로,
// bearerAuth 스킴과 전역 security를 주입하고 공개 API만 오퍼레이션 단위로 해제한다.
// 또한 Swagger UI 딥링크가 해시를 '/'로 분해해 operationId의 '/'를 해석하지 못하므로 '-'로 정규화하고,
// 전부 'api' 하나로 묶이는 태그를 REST Docs 목차와 같은 모듈 단위로 재배정한다
val postProcessOpenApiSpec = tasks.register("postProcessOpenApiSpec") {
	dependsOn("openapi3")
	group = "documentation"
	description = "openapi3.json에 보안 스킴과 모듈 태그를 주입하고 operationId를 정규화한다"
	val specFile = layout.buildDirectory.file("api-spec/openapi3.json")
	val publicPaths = listOf("/api/health", "/api/users/social/{provider}", "/api/users/token/refresh")
	val httpMethods = setOf("get", "post", "put", "patch", "delete", "head", "options")
	// REST Docs 스니펫 식별자의 모듈 접두어 → Swagger UI 그룹 태그. 선언 순서가 화면 표시 순서다
	val moduleTags = linkedMapOf(
		"user" to "Auth",
		"media" to "Media",
		"onboarding" to "Onboarding",
		"health" to "Health",
		"training" to "Training",
		"ai" to "AI",
	)
	doLast {
		val file = specFile.get().asFile
		require(file.exists()) { "openapi3.json이 없습니다. ./gradlew openapi3 를 먼저 실행하세요." }

		@Suppress("UNCHECKED_CAST")
		val spec = JsonSlurper().parse(file) as MutableMap<String, Any?>

		// UI가 API 서버 자신에게서 서빙되므로 상대 서버로 두면 Try it out이 현재 오리진을 향한다
		spec["servers"] = listOf(mapOf("url" to "/"))

		@Suppress("UNCHECKED_CAST")
		val components = spec.getOrPut("components") { mutableMapOf<String, Any?>() } as MutableMap<String, Any?>
		components["securitySchemes"] = mapOf(
			"bearerAuth" to mapOf("type" to "http", "scheme" to "bearer", "bearerFormat" to "JWT")
		)
		spec["security"] = listOf(mapOf("bearerAuth" to emptyList<String>()))

		@Suppress("UNCHECKED_CAST")
		val paths = spec["paths"] as? MutableMap<String, Any?> ?: mutableMapOf()
		publicPaths.forEach { path ->
			@Suppress("UNCHECKED_CAST")
			(paths[path] as? MutableMap<String, Any?>)?.forEach { (method, operation) ->
				if (method in httpMethods) {
					@Suppress("UNCHECKED_CAST")
					(operation as MutableMap<String, Any?>)["security"] = emptyList<Any>()
				}
			}
		}

		paths.values.forEach { pathItem ->
			@Suppress("UNCHECKED_CAST")
			(pathItem as? MutableMap<String, Any?>)?.forEach { (method, op) ->
				if (method in httpMethods) {
					@Suppress("UNCHECKED_CAST")
					val operation = op as MutableMap<String, Any?>
					val operationId = operation["operationId"] as? String ?: return@forEach
					operation["tags"] = listOf(moduleTags[operationId.substringBefore('/')] ?: "api")
					operation["operationId"] = operationId.replace('/', '-')
				}
			}
		}
		spec["tags"] = moduleTags.values.map { mapOf("name" to it) }

		file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(spec)))
	}
}

// openapi3 태스크는 플러그인이 afterEvaluate에서 등록하므로 여기서도 afterEvaluate로 참조한다
afterEvaluate {
	tasks.named("openapi3") { finalizedBy(postProcessOpenApiSpec) }
}

// 배포 jar에는 스펙이 반드시 포함되어야 하므로 bootJar가 스펙 생성 체인(test → openapi3 → 후처리)을 강제한다.
// 로컬 실행(IDE Run·bootRun)은 jar를 거치지 않고 WebConfig가 build/api-spec/의 파일을 직접 서빙한다
tasks.bootJar {
	dependsOn(postProcessOpenApiSpec)
	from(layout.buildDirectory.file("api-spec/openapi3.json")) { into("BOOT-INF/classes/static/swagger-ui") }
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required = true
	}
}

// 커버리지 검증 대상에서 부트스트랩·설정 클래스는 제외한다
val jacocoExcludes = listOf(
	"**/MeongcoachApplication*",
	"**/shared/config/**",
)

tasks.jacocoTestCoverageVerification {
	dependsOn(tasks.test)
	classDirectories.setFrom(
		sourceSets.main.get().output.asFileTree.matching {
			exclude(jacocoExcludes)
		}
	)
	violationRules {
		rule {
			limit {
				counter = "LINE"
				value = "COVEREDRATIO"
				minimum = "0.70".toBigDecimal()
			}
		}
	}
}
