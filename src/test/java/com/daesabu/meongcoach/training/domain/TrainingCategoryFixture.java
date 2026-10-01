package com.daesabu.meongcoach.training.domain;

import java.util.Objects;
import org.springframework.test.util.ReflectionTestUtils;

public final class TrainingCategoryFixture {

	private TrainingCategoryFixture() {
	}

	public static TrainingCategory create(String title, int sortOrder, String description, String iconUrl) {
		TrainingCategory category = new TrainingCategory();
		ReflectionTestUtils.setField(category, "title", title);
		ReflectionTestUtils.setField(category, "sortOrder", sortOrder);
		ReflectionTestUtils.setField(category, "description", Objects.requireNonNullElse(description, ""));
		ReflectionTestUtils.setField(category, "iconUrl", Objects.requireNonNullElse(iconUrl, ""));
		return category;
	}
}
