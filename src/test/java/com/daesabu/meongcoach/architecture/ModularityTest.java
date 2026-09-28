package com.daesabu.meongcoach.architecture;

import com.daesabu.meongcoach.MeongcoachApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith 모듈 경계 검증과 모듈 구조 문서 생성.
 */
class ModularityTest {

	private static final ApplicationModules MODULES = ApplicationModules.of(MeongcoachApplication.class);

	@Test
	void 모듈_간_경계를_위반하지_않는다() {
		MODULES.verify();
	}

	// 모듈 의존 다이어그램(PlantUML)과 모듈별 설명(모듈 캔버스)을 build/spring-modulith-docs에 만든다
	@Test
	void 모듈_구조_문서를_생성한다() {
		new Documenter(MODULES).writeDocumentation();
	}
}
