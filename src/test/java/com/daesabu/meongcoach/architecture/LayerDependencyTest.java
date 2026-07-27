package com.daesabu.meongcoach.architecture;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 계층 의존 방향 검증. 모듈 내부 의존은 adapter → application → domain 단방향이어야 한다.
 */
@DisplayName("계층 의존 방향 검증")
class LayerDependencyTest {

	private static final JavaClasses CLASSES = new ClassFileImporter()
			.withImportOption(new ImportOption.DoNotIncludeTests())
			.importPackages("com.daesabu.meongcoach");

	// 아직 클래스가 없는 계층이 있어도 통과하도록 withOptionalLayers(true)를 적용한다
	@Test
	@DisplayName("모듈 내부 의존은 adapter → application → domain 방향을 따른다")
	void layersFollowDependencyDirection() {
		layeredArchitecture()
				.consideringOnlyDependenciesInLayers()
				.withOptionalLayers(true)
				.layer("Adapter").definedBy("..adapter..")
				.layer("Application").definedBy("..application..")
				.layer("Domain").definedBy("..domain..")
				.whereLayer("Adapter").mayNotBeAccessedByAnyLayer()
				.whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter")
				.whereLayer("Domain").mayOnlyBeAccessedByLayers("Adapter", "Application")
				.check(CLASSES);
	}

	@Test
	@DisplayName("모듈 간 순환 의존이 없다")
	void modulesAreFreeOfCycles() {
		slices()
				.matching("com.daesabu.meongcoach.(*)..")
				.should().beFreeOfCycles()
				.check(CLASSES);
	}
}
