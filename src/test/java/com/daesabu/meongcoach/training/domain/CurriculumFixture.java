package com.daesabu.meongcoach.training.domain;

import java.util.Objects;
import org.springframework.test.util.ReflectionTestUtils;

public final class CurriculumFixture {

	private CurriculumFixture() {
	}

	public static Curriculum create(Topic topic, String title, int sortOrder, String thumbnailUrl, String description) {
		return create(topic, title, sortOrder, thumbnailUrl, description, false);
	}

	public static Curriculum create(Topic topic, String title, int sortOrder, String thumbnailUrl, String description,
			boolean isPremium) {
		Curriculum curriculum = new Curriculum();
		ReflectionTestUtils.setField(curriculum, "topic", topic);
		ReflectionTestUtils.setField(curriculum, "title", title);
		ReflectionTestUtils.setField(curriculum, "sortOrder", sortOrder);
		ReflectionTestUtils.setField(curriculum, "thumbnailUrl", Objects.requireNonNullElse(thumbnailUrl, ""));
		ReflectionTestUtils.setField(curriculum, "description", Objects.requireNonNullElse(description, ""));
		ReflectionTestUtils.setField(curriculum, "isPremium", isPremium);
		return curriculum;
	}
}
