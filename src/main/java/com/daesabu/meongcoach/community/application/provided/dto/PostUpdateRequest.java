package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.community.domain.MentionCommand;
import com.daesabu.meongcoach.community.domain.PostMediaCommand;
import com.daesabu.meongcoach.community.domain.PostUpdateCommand;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PostUpdateRequest(String content, @Valid MentionRequest mention,
		List<@Valid PostMediaRequest> media) {

	public PostUpdateCommand toCommand() {
		MentionCommand mentionCommand = Optional.ofNullable(mention)
				.map(MentionRequest::toCommand)
				.orElse(null);
		List<PostMediaCommand> mediaCommands = Objects.requireNonNullElse(media, List.<PostMediaRequest>of())
				.stream()
				.map(PostMediaRequest::toCommand)
				.toList();
		return new PostUpdateCommand(content, mentionCommand, mediaCommands);
	}
}
