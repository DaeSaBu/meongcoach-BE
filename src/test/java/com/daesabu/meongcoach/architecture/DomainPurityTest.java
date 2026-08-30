package com.daesabu.meongcoach.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

/**
 * 도메인 순수성과 애노테이션 적용 위치 검증.
 */
class DomainPurityTest {

	private static final JavaClasses CLASSES = new ClassFileImporter()
			.withImportOption(new ImportOption.DoNotIncludeTests())
			.importPackages("com.daesabu.meongcoach");

	// JPA 매핑(jakarta.persistence)은 허용하지만 스프링 의존은 허용하지 않는다.
	// package-info는 모듈 경계 메타데이터(@NamedInterface)만 담으므로 예외로 둔다
	@Test
	void domain은_스프링에_의존하지_않는다() {
		noClasses()
				.that().resideInAPackage("..domain..")
				.and().doNotHaveSimpleName("package-info")
				.should().dependOnClassesThat().resideInAPackage("org.springframework..")
				.check(CLASSES);
	}

	@Test
	void RestController는_Controller_접미사를_가진다() {
		classes()
				.that().areAnnotatedWith(RestController.class)
				.should().haveSimpleNameEndingWith("Controller")
				.check(CLASSES);
	}

	@Test
	void RestController는_adapter_webapi_패키지에_둔다() {
		classes()
				.that().areAnnotatedWith(RestController.class)
				.should().resideInAPackage("..adapter.webapi..")
				.check(CLASSES);
	}
}
