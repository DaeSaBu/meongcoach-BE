package com.daesabu.meongcoach.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class AppVersionFilterTest {

	private final AppVersionFilter filter = new AppVersionFilter(null, null);

	@Test
	void 관리자_API_경로는_앱_버전_검사를_건너뛴다() {
		assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/admin/graduations/submissions")))
				.isTrue();
	}

	@Test
	void 헬스_체크_경로는_앱_버전_검사를_건너뛴다() {
		assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/health"))).isTrue();
	}

	@Test
	void 일반_API_경로는_앱_버전을_검사한다() {
		assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/training/graduations"))).isFalse();
	}
}
