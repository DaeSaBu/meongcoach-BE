package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.community.domain.MentionCommand;
import com.daesabu.meongcoach.community.domain.PostMediaCommand;
import com.daesabu.meongcoach.community.domain.PostRegisterCommand;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PostRegisterRequest(String content, @Valid MentionRequest mention,
		List<@Valid PostMediaRequest> media) {

	public PostRegisterCommand toCommand() {
		MentionCommand mentionCommand = Optional.ofNullable(mention)
				.map(MentionRequest::toCommand)
				.orElse(null);
		List<PostMediaCommand> mediaCommands = Objects.requireNonNullElse(media, List.<PostMediaRequest>of())
				.stream()
				.map(PostMediaRequest::toCommand)
				.toList();
		return new PostRegisterCommand(content, mentionCommand, mediaCommands);
	}
}
