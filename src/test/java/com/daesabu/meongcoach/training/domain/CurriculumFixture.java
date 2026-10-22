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
		return create(topic, title, sortOrder, thumbnailUrl, description, isPremium, null);
	}

	public static Curriculum create(Topic topic, String title, int sortOrder, String thumbnailUrl, String description,
			boolean isPremium, Expert expert) {
		Curriculum curriculum = new Curriculum();
		ReflectionTestUtils.setField(curriculum, "topic", topic);
		ReflectionTestUtils.setField(curriculum, "expert", expert);
		ReflectionTestUtils.setField(curriculum, "title", title);
		ReflectionTestUtils.setField(curriculum, "sortOrder", sortOrder);
		ReflectionTestUtils.setField(curriculum, "thumbnailUrl", Objects.requireNonNullElse(thumbnailUrl, ""));
		ReflectionTestUtils.setField(curriculum, "description", Objects.requireNonNullElse(description, ""));
		ReflectionTestUtils.setField(curriculum, "isPremium", isPremium);
		return curriculum;
	}

	public static Curriculum create(Topic topic, Long id) {
		Curriculum curriculum = create(topic, "커리큘럼", 1, null, null);
		ReflectionTestUtils.setField(curriculum, "id", id);
		topic.getCurriculums().add(curriculum);
		return curriculum;
	}
}
