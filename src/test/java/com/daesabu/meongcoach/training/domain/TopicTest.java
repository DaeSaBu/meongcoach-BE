package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import org.junit.jupiter.api.Test;

class TopicTest {

	@Test
	void 카테고리가_요구하는_이용권을_반환한다() {
		Topic topic = topicIn(EntitlementType.PUPPY);

		assertThat(topic.findRequiredEntitlementType()).contains(EntitlementType.PUPPY);
	}

	@Test
	void 카테고리가_이용권을_요구하지_않으면_빈_값을_반환한다() {
		Topic topic = topicIn(null);

		assertThat(topic.findRequiredEntitlementType()).isEmpty();
	}

	private Topic topicIn(EntitlementType requiredEntitlementType) {
		TrainingCategory category = TrainingCategoryFixture.create("퍼피 교육", 1, null, null, requiredEntitlementType);
		return TopicFixture.create(category, "앉아", 1, null, null, null);
	}
}
