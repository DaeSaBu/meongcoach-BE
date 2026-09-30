package com.daesabu.meongcoach.training.domain;

import java.util.Objects;
import org.springframework.test.util.ReflectionTestUtils;

public final class TopicFixture {

	private TopicFixture() {
	}

	public static Topic create(TrainingCategory trainingCategory, String title, int sortOrder, String description,
			String detail, String iconUrl) {
		Topic topic = new Topic();
		ReflectionTestUtils.setField(topic, "trainingCategory", trainingCategory);
		ReflectionTestUtils.setField(topic, "title", title);
		ReflectionTestUtils.setField(topic, "sortOrder", sortOrder);
		ReflectionTestUtils.setField(topic, "description", Objects.requireNonNullElse(description, ""));
		ReflectionTestUtils.setField(topic, "detail", Objects.requireNonNullElse(detail, ""));
		ReflectionTestUtils.setField(topic, "iconUrl", Objects.requireNonNullElse(iconUrl, ""));
		return topic;
	}
}
