package com.daesabu.meongcoach.architecture;

import com.daesabu.meongcoach.MeongcoachApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Spring Modulith 모듈 경계 검증. 모듈 간 domain 직접 참조가 있으면 실패한다.
 */
class ModularityTest {

	@Test
	void 모듈_간_경계를_위반하지_않는다() {
		ApplicationModules.of(MeongcoachApplication.class).verify();
	}
}
