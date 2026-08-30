package com.daesabu.meongcoach.architecture;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * 모듈 내부 계층 의존 방향 검증. 의존은 항상 adapter → application → domain 이어야 한다.
 */
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
}
