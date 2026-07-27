package com.daesabu.meongcoach.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

/**
 * 도메인 순수성과 애노테이션 적용 위치 검증.
 */
@DisplayName("도메인 순수성 검증")
class DomainPurityTest {

	private static final JavaClasses CLASSES = new ClassFileImporter()
			.withImportOption(new ImportOption.DoNotIncludeTests())
			.importPackages("com.daesabu.meongcoach");

	// JPA 매핑(jakarta.persistence)은 허용하지만 스프링 의존은 허용하지 않는다
	@Test
	@DisplayName("domain은 스프링에 의존하지 않는다")
	void domainDoesNotDependOnSpring() {
		noClasses()
				.that().resideInAPackage("..domain..")
				.should().dependOnClassesThat().resideInAPackage("org.springframework..")
				.check(CLASSES);
	}

	@Test
	@DisplayName("RestController는 Controller 접미사를 가진다")
	void restControllersEndWithController() {
		classes()
				.that().areAnnotatedWith(RestController.class)
				.should().haveSimpleNameEndingWith("Controller")
				.check(CLASSES);
	}

	@Test
	@DisplayName("RestController는 adapter.webapi 패키지에 둔다")
	void restControllersResideInWebapi() {
		classes()
				.that().areAnnotatedWith(RestController.class)
				.should().resideInAPackage("..adapter.webapi..")
				.check(CLASSES);
	}
}
