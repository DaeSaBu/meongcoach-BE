package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.community.domain.PostMediaCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Objects;

public record CommentRegisterRequest(String content, @Positive Long parentId, List<@Valid PostMediaRequest> media) {

	public CommentRegisterRequest(String content, Long parentId) {
		this(content, parentId, null);
	}

	public List<PostMediaCommand> toMediaCommands() {
		return Objects.requireNonNullElse(media, List.<PostMediaRequest>of())
				.stream()
				.map(PostMediaRequest::toCommand)
				.toList();
	}
}
