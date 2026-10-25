package com.daesabu.meongcoach.community.application.provided.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.Objects;

public record CommentListRequest(@Positive Long cursor, @Min(1) @Max(50) Integer size) {

	public static final int DEFAULT_SIZE = 20;

	public CommentListRequest {
		size = Objects.requireNonNullElse(size, DEFAULT_SIZE);
	}
}
