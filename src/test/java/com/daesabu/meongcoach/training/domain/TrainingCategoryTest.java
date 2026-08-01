package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TrainingCategory 도메인")
class TrainingCategoryTest {

	@Test
	@DisplayName("생성하면 설명과 아이콘 URL이 설정된다")
	void createSetsDescriptionAndIconUrl() {
		TrainingCategory category = TrainingCategory.create(
				"기본 교육", 1, "기본기를 배우는 교육", "https://example.com/basic.png"
		);

		assertThat(category.getDescription()).isEqualTo("기본기를 배우는 교육");
		assertThat(category.getIconUrl()).isEqualTo("https://example.com/basic.png");
	}

	@Test
	@DisplayName("설명과 아이콘 URL이 없으면 빈 문자열로 설정한다")
	void createSetsEmptyStringsWhenDescriptionAndIconUrlAreNull() {
		TrainingCategory category = TrainingCategory.create("기본 교육", 1, null, null);

		assertThat(category.getDescription()).isEmpty();
		assertThat(category.getIconUrl()).isEmpty();
	}
}
