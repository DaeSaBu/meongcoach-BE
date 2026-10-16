package com.daesabu.meongcoach.training.domain;

import java.util.Objects;
import org.springframework.test.util.ReflectionTestUtils;

public final class ExpertFixture {

	private ExpertFixture() {
	}

	public static Expert create(String name, String title, String description, String imageUrl) {
		Expert expert = new Expert();
		ReflectionTestUtils.setField(expert, "name", name);
		ReflectionTestUtils.setField(expert, "title", title);
		ReflectionTestUtils.setField(expert, "description", Objects.requireNonNullElse(description, ""));
		ReflectionTestUtils.setField(expert, "imageUrl", Objects.requireNonNullElse(imageUrl, ""));
		return expert;
	}
}
