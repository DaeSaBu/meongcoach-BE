package com.daesabu.meongcoach.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RestController;

class AnnotationPatternTest {

	private static final JavaClasses CLASSES = new ClassFileImporter()
			.withImportOption(new ImportOption.DoNotIncludeTests())
			.importPackages("com.daesabu.meongcoach");

	@Test
	void Entity는_domain에만_둔다() {
		classes()
				.that().areAnnotatedWith(Entity.class)
				.should().resideInAPackage("..domain..")
				.check(CLASSES);
	}

	@Test
	void RestController는_adapter_webapi에만_둔다() {
		classes()
				.that().areAnnotatedWith(RestController.class)
				.should().resideInAPackage("..adapter.webapi..")
				.allowEmptyShould(true)
				.check(CLASSES);
	}


	@Test
	void domain은_Assert_외에는_스프링에_의존하지_않는다() {
		noClasses()
				.that().resideInAPackage("..domain..")
				.and().doNotHaveSimpleName("package-info")
				.should().dependOnClassesThat(resideInAPackage("org.springframework..").and(not(equivalentTo(Assert.class))))
				.check(CLASSES);
	}

	@Test
	void Assert는_domain에서만_사용한다() {
		noClasses()
				.that().resideOutsideOfPackage("..domain..")
				.should().dependOnClassesThat().belongToAnyOf(Assert.class)
				.check(CLASSES);
	}
}
