package com.daesabu.meongcoach.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;


class LayerDependencyTest {

	private static final JavaClasses CLASSES = new ClassFileImporter()
			.withImportOption(new ImportOption.DoNotIncludeTests())
			.importPackages("com.daesabu.meongcoach");

	@Test
	void 계층_의존은_adapter에서_application_domain_방향으로만_흐른다() {
		layeredArchitecture()
				.consideringOnlyDependenciesInLayers()
				.layer("Adapter").definedBy("..adapter..")
				.layer("Application").definedBy("..application..")
				.layer("Domain").definedBy("..domain..")
				.whereLayer("Adapter").mayNotBeAccessedByAnyLayer()
				.whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter")
				.whereLayer("Domain").mayOnlyBeAccessedByLayers("Adapter", "Application")
				.check(CLASSES);
	}

	@Test
	void 최상위_모듈_사이에_순환_의존이_없다() {
		slices()
				.matching("com.daesabu.meongcoach.(*)..")
				.should().beFreeOfCycles()
				.check(CLASSES);
	}
	@Test
	void 알림_모듈은_상담_모듈에_의존하지_않는다() {
		noClasses().that().resideInAPackage("com.daesabu.meongcoach.notification..")
				.should().dependOnClassesThat().resideInAPackage("com.daesabu.meongcoach.support..")
				.check(CLASSES);
	}
}
