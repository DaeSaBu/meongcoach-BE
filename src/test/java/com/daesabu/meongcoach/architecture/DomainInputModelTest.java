package com.daesabu.meongcoach.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * 도메인 입력 모델 네이밍 검증. domain 루트의 record는 ~Command 접미사를 가져야 한다.
 */
@AnalyzeClasses(packages = "com.daesabu.meongcoach", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainInputModelTest {

	// "..domain" 끝 매칭이라 domain/vo의 값 객체 record(Email)는 규칙 대상에서 제외된다
	@ArchTest
	static final ArchRule 도메인_입력_모델_커맨드_네이밍 = classes()
			.that().resideInAPackage("..domain")
			.and().areRecords()
			.should().haveSimpleNameEndingWith("Command");

	@ArchTest
	static final ArchRule 도메인_요청응답_접미사_금지 = noClasses()
			.that().resideInAPackage("..domain..")
			.should().haveSimpleNameEndingWith("Request")
			.orShould().haveSimpleNameEndingWith("Response");
}
