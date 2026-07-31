package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MBTI 조회 서비스")
class MbtiFinderServiceTest {

	private final MbtiFinderService service = new MbtiFinderService();

	@Test
	@DisplayName("16가지 MBTI 코드를 모두 반환한다")
	void findAllCodesReturnsAllSixteenMbtiCodes() {
		List<String> codes = service.findAllCodes();

		assertThat(codes).hasSize(16).contains("ISTJ", "ENFP");
	}
}
