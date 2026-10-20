package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.community.domain.MentionCommand;
import com.daesabu.meongcoach.community.domain.PostRegisterCommand;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

public record PostRegisterRequest(String content, @Valid MentionRequest mention) {

	public PostRegisterCommand toCommand() {
		MentionCommand mentionCommand = Optional.ofNullable(mention)
				.map(MentionRequest::toCommand)
				.orElse(null);
		return new PostRegisterCommand(content, mentionCommand, List.of());
	}
}
