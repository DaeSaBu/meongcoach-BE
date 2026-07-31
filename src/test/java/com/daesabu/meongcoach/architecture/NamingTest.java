package com.daesabu.meongcoach.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

/**
 * 아키텍처 역할별 클래스 네이밍 검증. code-convention.md의 네이밍 표를 강제한다.
 */
@DisplayName("아키텍처 역할별 네이밍 검증")
class NamingTest {

	private static final JavaClasses CLASSES = new ClassFileImporter()
			.withImportOption(new ImportOption.DoNotIncludeTests())
			.importPackages("com.daesabu.meongcoach");

	// 컨트롤러가 하나도 없어도 통과하도록 allowEmptyShould(true)를 적용한다
	@Test
	@DisplayName("RestController는 Controller 접미사를 가진다")
	void controllersEndWithControllerSuffix() {
		classes()
				.that().areAnnotatedWith(RestController.class)
				.should().haveSimpleNameEndingWith("Controller")
				.allowEmptyShould(true)
				.check(CLASSES);
	}
}
