package com.daesabu.meongcoach.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

/**
 * 애노테이션 적용 위치 검증. 웹 기술은 adapter/webapi에만, domain은 스프링에 의존하지 않는다.
 */
class AnnotationPatternTest {

	private static final JavaClasses CLASSES = new ClassFileImporter()
			.withImportOption(new ImportOption.DoNotIncludeTests())
			.importPackages("com.daesabu.meongcoach");

	// 컨트롤러가 하나도 없어도 통과하도록 allowEmptyShould(true)를 적용한다
	@Test
	void RestController는_adapter_webapi에만_둔다() {
		classes()
				.that().areAnnotatedWith(RestController.class)
				.should().resideInAPackage("..adapter.webapi..")
				.allowEmptyShould(true)
				.check(CLASSES);
	}

	// package-info는 모듈 경계 메타데이터(@NamedInterface)만 담으므로 예외로 둔다
	@Test
	void domain은_스프링에_의존하지_않는다() {
		noClasses()
				.that().resideInAPackage("..domain..")
				.and().doNotHaveSimpleName("package-info")
				.should().dependOnClassesThat().resideInAPackage("org.springframework..")
				.check(CLASSES);
	}
}
