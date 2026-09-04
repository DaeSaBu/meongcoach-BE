package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TrainingCategoryTest {

	@Test
	void 생성하면_설명과_아이콘_URL이_설정된다() {
		TrainingCategory category = TrainingCategory.create(
				"기본 교육", 1, "기본기를 배우는 교육", "https://example.com/basic.png"
		);

		assertThat(category.getDescription()).isEqualTo("기본기를 배우는 교육");
		assertThat(category.getIconUrl()).isEqualTo("https://example.com/basic.png");
	}

	@Test
	void 설명과_아이콘_URL이_없으면_빈_문자열로_설정한다() {
		TrainingCategory category = TrainingCategory.create("기본 교육", 1, null, null);

		assertThat(category.getDescription()).isEmpty();
		assertThat(category.getIconUrl()).isEmpty();
	}
}
