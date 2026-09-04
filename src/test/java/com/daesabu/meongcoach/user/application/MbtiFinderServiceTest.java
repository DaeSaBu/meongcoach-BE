package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class MbtiFinderServiceTest {

	private final MbtiFinderService service = new MbtiFinderService();

	@Test
	void MBTI_코드_16가지를_모두_반환한다() {
		List<String> codes = service.findAllCodes();

		assertThat(codes).hasSize(16).contains("ISTJ", "ENFP");
	}
}
