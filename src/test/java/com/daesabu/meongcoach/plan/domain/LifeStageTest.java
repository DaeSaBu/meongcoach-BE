package com.daesabu.meongcoach.plan.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LifeStageTest {

	@ParameterizedTest(name = "{0}개월은 {1}")
	@CsvSource({
			"0, PUPPY",
			"11, PUPPY",
			"12, JUNIOR",
			"23, JUNIOR",
			"24, ADULT",
			"95, ADULT",
			"96, SENIOR",
			"200, SENIOR",
	})
	void 월령으로_생애주기를_정한다(int months, LifeStage expected) {
		assertThat(LifeStage.of(months)).isEqualTo(expected);
	}

	@Test
	void 월령을_모르면_강아지_단계로_본다() {
		assertThat(LifeStage.of(null)).isEqualTo(LifeStage.PUPPY);
	}

	@Test
	void 생애주기를_이용권_종류로_바꾼다() {
		assertThat(LifeStage.PUPPY.toEntitlementType()).isEqualTo(EntitlementType.PUPPY);
		assertThat(LifeStage.JUNIOR.toEntitlementType()).isEqualTo(EntitlementType.JUNIOR);
		assertThat(LifeStage.ADULT.toEntitlementType()).isEqualTo(EntitlementType.ADULT);
		assertThat(LifeStage.SENIOR.toEntitlementType()).isEqualTo(EntitlementType.SENIOR);
	}

	@Test
	void 우선순위는_자기_단계_다음_앞_단계_역순_다음_뒤_단계_순이다() {
		assertThat(LifeStage.SENIOR.priority())
				.containsExactly(LifeStage.SENIOR, LifeStage.ADULT, LifeStage.JUNIOR, LifeStage.PUPPY);
		assertThat(LifeStage.JUNIOR.priority())
				.containsExactly(LifeStage.JUNIOR, LifeStage.PUPPY, LifeStage.ADULT, LifeStage.SENIOR);
		assertThat(LifeStage.PUPPY.priority())
				.containsExactly(LifeStage.PUPPY, LifeStage.JUNIOR, LifeStage.ADULT, LifeStage.SENIOR);
	}

	@ParameterizedTest(name = "{0}개월은 {1}")
	@CsvSource({
			"0, 0개월",
			"7, 7개월",
			"11, 11개월",
			"12, 1살",
			"18, 1살 6개월",
			"24, 2살",
			"100, 8살 4개월",
	})
	void 월령을_표현한다(int months, String expected) {
		assertThat(LifeStage.formatAge(months, false)).isEqualTo(expected);
	}

	@Test
	void 추정한_월령은_약과_추정을_붙여_표현한다() {
		assertThat(LifeStage.formatAge(9, true)).isEqualTo("약 9개월(추정)");
		assertThat(LifeStage.formatAge(18, true)).isEqualTo("약 1살 6개월(추정)");
	}

	@Test
	void 월령을_모르면_빈_문자열로_표현한다() {
		assertThat(LifeStage.formatAge(null, false)).isEmpty();
		assertThat(LifeStage.formatAge(null, true)).isEmpty();
	}

	@ParameterizedTest(name = "{0}개월은 {1}")
	@CsvSource({
			"0, 3개월 미만",
			"2, 3개월 미만",
			"3, 3~5개월",
			"5, 3~5개월",
			"6, 6~11개월",
			"11, 6~11개월",
			"12, 1살(12~23개월)",
			"23, 1살(12~23개월)",
			"24, 2~4살(24~59개월)",
			"59, 2~4살(24~59개월)",
			"60, 5~7살(60~95개월)",
			"95, 5~7살(60~95개월)",
			"96, 8살 이상(96개월 이상)",
	})
	void 월령이_속한_구간_문구를_구한다(int months, String expected) {
		assertThat(LifeStage.ageRange(months)).isEqualTo(expected);
	}

	@Test
	void 월령을_모르면_구간_문구도_빈_문자열이다() {
		assertThat(LifeStage.ageRange(null)).isEmpty();
	}
}
