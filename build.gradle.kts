plugins {
	java
	jacoco
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.asciidoctor.jvm.convert") version "4.0.4"
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

dependencies {
	implementation("org.springframework.boot:spring-boot-h2console")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("com.h2database:h2")
	annotationProcessor("org.projectlombok:lombok")
	asciidoctorExt("org.springframework.restdocs:spring-restdocs-asciidoctor")
	testImplementation("org.springframework.boot:spring-boot-restdocs")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
	testImplementation("com.tngtech.archunit:archunit-junit5:1.4.2")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
}

val snippetsDir = layout.buildDirectory.dir("generated-snippets")

tasks.withType<Test> {
	useJUnitPlatform()
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
