package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.community.domain.MentionCommand;
import com.daesabu.meongcoach.community.domain.PostWriteCommand;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

public record PostRegisterRequest(String content, @Valid MentionRequest mention) {

	public PostWriteCommand toCommand() {
		MentionCommand mentionCommand = Optional.ofNullable(mention)
				.map(MentionRequest::toCommand)
				.orElse(null);
		return new PostWriteCommand(content, mentionCommand, List.of());
	}
}
