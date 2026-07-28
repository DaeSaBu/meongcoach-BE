package com.daesabu.meongcoach.architecture;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 모듈 내부 계층 의존 방향 검증. 의존은 항상 adapter → application → domain 이어야 한다.
 */
@DisplayName("계층 의존 방향 검증")
class LayerDependencyTest {

	private static final JavaClasses CLASSES = new ClassFileImporter()
			.withImportOption(new ImportOption.DoNotIncludeTests())
			.importPackages("com.daesabu.meongcoach");

	@Test
	@DisplayName("계층 의존은 adapter에서 application, domain 방향으로만 흐른다")
	void layersDependInOneDirection() {
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
	@DisplayName("최상위 모듈 사이에 순환 의존이 없다")
	void modulesAreFreeOfCycles() {
		slices()
				.matching("com.daesabu.meongcoach.(*)..")
				.should().beFreeOfCycles()
				.check(CLASSES);
	}
}
