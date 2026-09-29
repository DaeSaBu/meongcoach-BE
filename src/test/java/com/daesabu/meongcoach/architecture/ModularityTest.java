package com.daesabu.meongcoach.architecture;

import com.daesabu.meongcoach.MeongcoachApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;
import org.springframework.modulith.docs.Documenter.CanvasOptions;
import org.springframework.modulith.docs.Documenter.DiagramOptions;
import org.springframework.modulith.docs.Documenter.DiagramOptions.DiagramStyle;
import org.springframework.modulith.docs.Documenter.DiagramOptions.ElementsWithoutRelationships;

/**
 * Spring Modulith 모듈 경계 검증과 모듈 구조 문서 생성.
 */
class ModularityTest {

	private static final ApplicationModules MODULES = ApplicationModules.of(MeongcoachApplication.class);

	// 모듈 의존 다이어그램(PlantUML)과 모듈별 설명(모듈 캔버스)을 build/spring-modulith-docs에 만든다.
	// 배포 단위가 하나라 C4의 시스템·컨테이너 경계는 정보가 없으므로 UML로 그리고, 의존이 없는 모듈(health)도 보이게 한다
	private static final DiagramOptions DIAGRAM_OPTIONS = DiagramOptions.defaults()
			.withStyle(DiagramStyle.UML)
			.withElementsWithoutRelationships(ElementsWithoutRelationships.VISIBLE);

	@Test
	void 모듈_간_경계를_위반하지_않는다() {
		MODULES.verify();
	}

	@Test
	void 모듈_구조_문서를_생성한다() {
		new Documenter(MODULES).writeDocumentation(DIAGRAM_OPTIONS, CanvasOptions.defaults());
	}
}
