package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.community.domain.PostMediaCommand;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

public record CommentUpdateRequest(String content, List<@Valid PostMediaRequest> media) {

	public CommentUpdateRequest(String content) {
		this(content, null);
	}

	public List<PostMediaCommand> toMediaCommands() {
		return Optional.ofNullable(media)
				.map(requests -> requests.stream()
						.map(PostMediaRequest::toCommand)
						.toList())
				.orElse(null);
	}
}
