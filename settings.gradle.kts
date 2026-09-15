plugins {
	// 부하 테스트 소스셋이 요구하는 JDK 21이 로컬에 없으면 자동으로 내려받는다. Gradle 9는 리졸버 플러그인 없이는 툴체인을 설치하지 않는다
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "meongcoach"
