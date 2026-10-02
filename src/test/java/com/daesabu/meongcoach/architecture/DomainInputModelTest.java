package com.daesabu.meongcoach.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * 도메인 입력 모델 네이밍 검증. 값 객체 record도 domain 루트에 있어 record 여부로는 입력 모델을 가릴 수 없으므로, ~Command 접미사는 검증하지 않고 웹 계층 접미사 유입만 막는다.
 */
class DomainInputModelTest {

	private static final JavaClasses CLASSES = new ClassFileImporter()
			.withImportOption(new ImportOption.DoNotIncludeTests())
			.importPackages("com.daesabu.meongcoach");

	@Test
	void domain_패키지에_Request나_Response_접미사_클래스를_두지_않는다() {
		noClasses()
				.that().resideInAPackage("..domain..")
				.should().haveSimpleNameEndingWith("Request")
				.orShould().haveSimpleNameEndingWith("Response")
				.check(CLASSES);
	}
}
