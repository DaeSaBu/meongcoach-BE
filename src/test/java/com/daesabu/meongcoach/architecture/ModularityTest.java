package com.daesabu.meongcoach.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import com.daesabu.meongcoach.MeongcoachApplication;

/**
 * Spring Modulith 모듈 경계 검증. 모듈 간 domain 직접 참조가 있으면 실패한다.
 */
class ModularityTest {

	@Test
	void verifyModuleBoundaries() {
		ApplicationModules.of(MeongcoachApplication.class).verify();
	}
}
