package com.daesabu.meongcoach.architecture;

import com.daesabu.meongcoach.MeongcoachApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModuleIdentifier;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;
import org.springframework.modulith.docs.Documenter.CanvasOptions;
import org.springframework.modulith.docs.Documenter.DiagramOptions;
import org.springframework.modulith.docs.Documenter.DiagramOptions.DiagramStyle;
import org.springframework.modulith.docs.Documenter.DiagramOptions.ElementsWithoutRelationships;

class ModularityTest {

	private static final ApplicationModules MODULES = ApplicationModules.of(MeongcoachApplication.class);

	private static final ApplicationModuleIdentifier SHARED = ApplicationModuleIdentifier.of("shared");

	private static final DiagramOptions DIAGRAM_OPTIONS = DiagramOptions.defaults()
			.withStyle(DiagramStyle.UML)
			.withElementsWithoutRelationships(ElementsWithoutRelationships.VISIBLE)
			.withExclusions(module -> module.getIdentifier().equals(SHARED));

	@Test
	void 모듈_간_경계를_위반하지_않는다() {
		MODULES.verify();
	}

	@Test
	void 모듈_구조_문서를_생성한다() {
		new Documenter(MODULES).writeDocumentation(DIAGRAM_OPTIONS, CanvasOptions.defaults());
	}
}
