package com.daesabu.meongcoach.community.application.provided.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.Objects;

public record PostListRequest(@Positive Long cursor, @Min(1) @Max(50) Integer size, @Positive Long lessonId) {

	public static final int DEFAULT_SIZE = 20;

	public PostListRequest {
		size = Objects.requireNonNullElse(size, DEFAULT_SIZE);
	}
}
