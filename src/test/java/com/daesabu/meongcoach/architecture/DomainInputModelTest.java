package com.daesabu.meongcoach.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * 도메인 입력 모델 네이밍 검증. domain 루트의 record는 ~Command 접미사를 가져야 한다.
 */
class DomainInputModelTest {

	private static final JavaClasses CLASSES = new ClassFileImporter()
			.withImportOption(new ImportOption.DoNotIncludeTests())
			.importPackages("com.daesabu.meongcoach");

	// "..domain" 끝 매칭이라 domain/vo의 값 객체 record(Email)는 규칙 대상에서 제외된다
	@Test
	void domain_루트의_입력_모델_record는_Command_접미사를_가진다() {
		classes()
				.that().resideInAPackage("..domain")
				.and().areRecords()
				.should().haveSimpleNameEndingWith("Command")
				.check(CLASSES);
	}

	@Test
	void domain_패키지에_Request_Response_접미사_클래스를_두지_않는다() {
		noClasses()
				.that().resideInAPackage("..domain..")
				.should().haveSimpleNameEndingWith("Request")
				.orShould().haveSimpleNameEndingWith("Response")
				.check(CLASSES);
	}
}
