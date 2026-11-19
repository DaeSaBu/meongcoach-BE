package com.daesabu.meongcoach.training.domain;

import java.util.Objects;
import org.springframework.test.util.ReflectionTestUtils;

public final class CurriculumFixture {

	private CurriculumFixture() {
	}

	public static Curriculum create(Topic topic, String title, int sortOrder, String thumbnailUrl, String description) {
		return create(topic, title, sortOrder, thumbnailUrl, description, null);
	}

	public static Curriculum create(Topic topic, String title, int sortOrder, String thumbnailUrl, String description,
			Expert expert) {
		Curriculum curriculum = new Curriculum();
		ReflectionTestUtils.setField(curriculum, "topic", topic);
		ReflectionTestUtils.setField(curriculum, "expert", expert);
		ReflectionTestUtils.setField(curriculum, "title", title);
		ReflectionTestUtils.setField(curriculum, "sortOrder", sortOrder);
		ReflectionTestUtils.setField(curriculum, "thumbnailUrl", Objects.requireNonNullElse(thumbnailUrl, ""));
		ReflectionTestUtils.setField(curriculum, "description", Objects.requireNonNullElse(description, ""));
		return curriculum;
	}

	public static Curriculum create(Topic topic, Long id) {
		Curriculum curriculum = create(topic, "커리큘럼", 1, null, null);
		ReflectionTestUtils.setField(curriculum, "id", id);
		topic.getCurriculums().add(curriculum);
		return curriculum;
	}
}
