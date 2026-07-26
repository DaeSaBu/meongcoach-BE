package com.daesabu.meongcoach.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 도메인 입력 모델 네이밍 검증. domain 루트의 record는 ~Command 접미사를 가져야 한다.
 */
@DisplayName("도메인 입력 모델 네이밍 검증")
class DomainInputModelTest {

	private static final JavaClasses CLASSES = new ClassFileImporter()
			.withImportOption(new ImportOption.DoNotIncludeTests())
			.importPackages("com.daesabu.meongcoach");

	// "..domain" 끝 매칭이라 domain/vo의 값 객체 record(Email)는 규칙 대상에서 제외된다
	@Test
	@DisplayName("domain 루트의 입력 모델 record는 Command 접미사를 가진다")
	void domainInputModelRecordsEndWithCommand() {
		classes()
				.that().resideInAPackage("..domain")
				.and().areRecords()
				.should().haveSimpleNameEndingWith("Command")
				.check(CLASSES);
	}

	@Test
	@DisplayName("domain 패키지에 Request·Response 접미사 클래스를 두지 않는다")
	void domainForbidsRequestAndResponseSuffix() {
		noClasses()
				.that().resideInAPackage("..domain..")
				.should().haveSimpleNameEndingWith("Request")
				.orShould().haveSimpleNameEndingWith("Response")
				.check(CLASSES);
	}
}
