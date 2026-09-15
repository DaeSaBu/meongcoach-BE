import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes

// 커버리지 제외 판별용 클래스 파일 파서. Java 25 클래스(major 69)를 읽으려면 ASM 9.8 이상이어야 한다
buildscript {
	repositories { mavenCentral() }
	dependencies { classpath("org.ow2.asm:asm:9.9") }
}

plugins {
	java
	jacoco
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.asciidoctor.jvm.convert") version "4.0.4"
	// 0.20.x가 Spring Boot 4.x / REST Docs 4.x 지원 라인이다
	id("com.epages.restdocs-api-spec") version "0.20.1"
	id("com.diffplug.spotless") version "8.8.0"
	id("org.flywaydb.flyway") version "12.7.0"
}

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

// `./gradlew flywayMigrate`로 마이그레이션 파일을 로컬 postgres(compose.yml)에 직접 검증할 때 쓴다.
// 앱 부팅 시 마이그레이션은 dev/prod 프로파일이 spring.flyway.*(DataSource 재사용)로 별도 수행한다.
// 앱(local 프로파일)의 create-drop과 섞이지 않도록 검증 전용 DB(compose/postgres-init.sql)를 쓴다
flyway {
	url = "jdbc:postgresql://localhost:5432/meongcoach_schema_check"
	user = "meongcoach_local"
	password = "meongcoach-local"
	locations = arrayOf("filesystem:src/main/resources/db/migration")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.modulith:spring-modulith-bom:2.1.0")
		// Spring Cloud AWS 4.x가 Spring Boot 4.x 지원 라인이다
		mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:4.1.0")
		// starter와 logback 연동의 버전이 어긋나면 런타임에 깨지므로 BOM으로 묶는다
		mavenBom("io.sentry:sentry-bom:8.54.0")
	}
}

dependencies {
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
	// 에러 모니터링. starter는 logback 연동을 포함하지 않아 따로 넣는다. error 로그가 Sentry 이벤트가 된다
	implementation("io.sentry:sentry-spring-boot-4-starter")
	implementation("io.sentry:sentry-logback")
	testImplementation("org.springframework.security:spring-security-test")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("org.postgresql:postgresql")
	// 테스트 DB. application-test.yml의 jdbc:tc: URL을 Testcontainers JDBC 드라이버가 해석해 PostgreSQL 컨테이너를 띄운다
	testRuntimeOnly("org.testcontainers:testcontainers-postgresql")
	// Spring Boot 4는 Flyway 자동 구성이 starter로 분리돼 있어 없으면 기동 시 마이그레이션이 실행되지 않는다
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.flywaydb:flyway-database-postgresql")
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

// ───────── 부하 테스트 (JMeter Java DSL) — docs/load-test.md ─────────
// test와 분리된 별도 소스셋. main 클래스에 의존하지 않고 HTTP로만 서버를 호출한다.
// check·test·jacoco·bootJar 어디에도 연결하지 않으므로 CI(`test jacocoTestCoverageVerification bootJar`)에서 실행되지 않는다
val loadTest: SourceSet by sourceSets.creating

// JMeter 5.6.3이 번들한 Groovy 3.0.20이 Java 23+ 클래스 파일을 읽지 못해(apache/jmeter#6402) Java 25 호환이 검증되지 않았다.
// 이 소스셋만 JDK 21로 컴파일·실행한다. 실험은 `-PloadTestJava=25`
val loadTestJavaVersion = JavaLanguageVersion.of((findProperty("loadTestJava") as String?) ?: "21")

dependencies {
	"loadTestImplementation"("us.abstracta.jmeter:jmeter-java-dsl:2.2.1") {
		// JMeter 아티팩트가 참조하는 bom pom을 Gradle이 해석하지 못해 DSL 가이드대로 제외한다
		exclude(group = "org.apache.jmeter", module = "bom")
		// Boot BOM이 slf4j를 2.x로 올리므로 JMeter가 가져오는 1.7 바인딩을 빼고 아래에서 2.x 바인딩을 넣는다
		exclude(group = "org.apache.logging.log4j", module = "log4j-slf4j-impl")
	}
	"loadTestRuntimeOnly"("org.apache.logging.log4j:log4j-slf4j2-impl")
	// 계정 준비 헬퍼의 JSON 처리. JMeter가 쓰는 Jackson 2 계열이라 main의 Jackson 3과 다르다
	"loadTestImplementation"("com.fasterxml.jackson.core:jackson-databind")
	"loadTestImplementation"("org.junit.jupiter:junit-jupiter")
	"loadTestImplementation"("org.assertj:assertj-core")
	"loadTestRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

tasks.named<JavaCompile>(loadTest.compileJavaTaskName) {
	javaCompiler = javaToolchains.compilerFor { languageVersion = loadTestJavaVersion }
}

val loadTestOutputDir = layout.buildDirectory.dir("load-test")

tasks.register<Test>("loadTest") {
	group = "verification"
	description = "JMeter Java DSL 부하 테스트를 실행한다. check에 포함되지 않으며 CI에서 실행하지 않는다"
	testClassesDirs = loadTest.output.classesDirs
	classpath = loadTest.runtimeClasspath
	javaLauncher = javaToolchains.launcherFor { languageVersion = loadTestJavaVersion }

	// jacoco 플러그인은 모든 Test 태스크에 에이전트를 붙인다. 부하 생성기를 느리게 할 뿐이므로 끈다
	extensions.configure<JacocoTaskExtension> { isEnabled = false }

	// 결과는 매번 새로 만든다
	outputs.upToDateWhen { false }

	// 실행 파라미터: 커맨드라인 -Dloadtest.* 를 포크된 JVM에 그대로 전달한다. 환경변수(LOADTEST_*)는 포크된 JVM이 상속한다
	listOf("baseUrl", "apiKey", "threads", "rampUp", "duration", "accounts", "lessonId")
		.map { "loadtest.$it" }
		.forEach { key -> providers.systemProperty(key).orNull?.let { systemProperty(key, it) } }
	systemProperty("loadtest.outputDir", loadTestOutputDir.get().asFile.absolutePath)
	// DSL이 JMeter 홈을 java.io.tmpdir 아래 임시 디렉터리로 만든다. build 아래로 두어 clean으로 정리되게 한다
	systemProperty("java.io.tmpdir", loadTestOutputDir.get().dir("tmp").asFile.absolutePath)
	// JMeter가 user.dir에 남기는 파일이 저장소 루트를 더럽히지 않게 한다
	workingDir = loadTestOutputDir.get().asFile

	reports.html.outputLocation = loadTestOutputDir.map { it.dir("junit") }
	reports.junitXml.outputLocation = loadTestOutputDir.map { it.dir("junit-xml") }
	testLogging {
		events("passed", "failed", "skipped")
		showStandardStreams = true
	}

	doFirst {
		loadTestOutputDir.get().dir("tmp").asFile.mkdirs()
		// tasks.withType<Test>가 주입한 값. Spring이 없는 클래스패스라 무해하지만 혼란을 막기 위해 걷어낸다
		systemProperties.remove("spring.profiles.active")
	}
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
	val publicPaths = listOf(
		"/api/health", "/api/auth/login/social/{provider}", "/api/auth/login/local", "/api/auth/token/refresh", "/api/auth/logout",
		// 인증 대신 공유 키 헤더를 쓰는 부하 테스트 전용 경로
		"/api/loadtest/accounts"
	)
	val httpMethods = setOf("get", "post", "put", "patch", "delete", "head", "options")
	// REST Docs 스니펫 식별자의 모듈 접두어 → Swagger UI 그룹 태그. 선언 순서가 화면 표시 순서다
	val moduleTags = linkedMapOf(
		"auth" to "Auth",
		"user" to "User",
		"media" to "Media",
		"onboarding" to "Onboarding",
		"health" to "Health",
		"training" to "Training",
		"ai" to "AI",
		"dog" to "Dog",
		"loadtest" to "LoadTest",
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

// 커버리지 대상에서 부트스트랩·설정 클래스와 enum(ErrorCode 포함)을 제외한다.
// enum은 상수 나열이라 커버리지를 채우기 위한 테스트를 강제하지 않는다. 변환 로직(from/of)은 사용처 테스트가 간접 검증한다
val jacocoExcludes = listOf(
	"**/MeongcoachApplication*",
	"**/shared/config/**",
)

// 이름 패턴으로는 enum을 가려낼 수 없으므로 컴파일된 클래스의 ACC_ENUM 플래그로 판별한다
fun isEnumClassFile(file: File): Boolean =
	file.extension == "class" && file.inputStream().use { (ClassReader(it).access and Opcodes.ACC_ENUM) != 0 }

// FileCollection.filter는 지연 평가되므로 컴파일 이후(태스크 실행 시점)에 클래스 파일을 읽는다
val jacocoClassDirectories = sourceSets.main.get().output.asFileTree
	.matching { exclude(jacocoExcludes) }
	.filter { !isEnumClassFile(it) }

// 리포트(CI의 PR 코멘트)와 검증이 같은 분모를 쓰도록 둘 다 같은 컬렉션을 사용한다
tasks.jacocoTestReport {
	dependsOn(tasks.test)
	classDirectories.setFrom(jacocoClassDirectories)
	reports {
		xml.required = true
	}
}

tasks.jacocoTestCoverageVerification {
	dependsOn(tasks.test)
	classDirectories.setFrom(jacocoClassDirectories)
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
