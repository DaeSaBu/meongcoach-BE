package com.daesabu.meongcoach.architecture;

import com.daesabu.meongcoach.MeongcoachApplication;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.SpringBean;
import org.springframework.modulith.docs.Documenter;
import org.springframework.modulith.docs.Documenter.CanvasOptions;
import org.springframework.modulith.docs.Documenter.DiagramOptions;
import org.springframework.modulith.docs.Documenter.DiagramOptions.DiagramStyle;
import org.springframework.modulith.docs.Documenter.DiagramOptions.ElementsWithoutRelationships;

class ModularityTest {

	private static final ApplicationModules MODULES = ApplicationModules.of(MeongcoachApplication.class);

	private static final DiagramOptions DIAGRAM_OPTIONS = DiagramOptions.defaults()
			.withStyle(DiagramStyle.UML)
			.withElementsWithoutRelationships(ElementsWithoutRelationships.VISIBLE);

	private static final CanvasOptions CANVAS_OPTIONS = CanvasOptions.defaults()
			.revealInternals()
			.groupingBy("외부 연동", inPackage(".adapter.integration"))
			.groupingBy("메시지 수신", inPackage(".adapter.consumer"));

	@Test
	void 모듈_간_경계를_위반하지_않는다() {
		MODULES.verify();
	}

	@Test
	void 모듈_구조_문서를_생성한다() {
		new Documenter(MODULES).writeDocumentation(DIAGRAM_OPTIONS, CANVAS_OPTIONS);
	}

	private static Predicate<SpringBean> inPackage(String packageSuffix) {
		return bean -> bean.getType().getPackageName().endsWith(packageSuffix);
	}
}
