package com.daesabu.meongcoach.training.domain;

import java.util.Objects;
import org.springframework.test.util.ReflectionTestUtils;

public final class CardFixture {

	private CardFixture() {
	}

	public static Card create(Lesson lesson, String title, int sortOrder, String instruction) {
		Card card = new Card();
		ReflectionTestUtils.setField(card, "lesson", lesson);
		ReflectionTestUtils.setField(card, "title", Objects.requireNonNullElse(title, ""));
		ReflectionTestUtils.setField(card, "sortOrder", sortOrder);
		ReflectionTestUtils.setField(card, "instruction", Objects.requireNonNullElse(instruction, ""));
		return card;
	}
}
