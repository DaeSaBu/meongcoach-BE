package com.daesabu.meongcoach.architecture;

import com.daesabu.meongcoach.MeongcoachApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Spring Modulith 모듈 경계 검증. 모듈 간 domain 직접 참조가 있으면 실패한다.
 */
@DisplayName("모듈 경계 검증")
class ModularityTest {

	@Test
	@DisplayName("모듈 간 경계를 위반하지 않는다")
	void verifyModuleBoundaries() {
		ApplicationModules.of(MeongcoachApplication.class).verify();
	}
}
