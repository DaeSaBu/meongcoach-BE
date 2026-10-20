package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.community.domain.MentionCommand;
import com.daesabu.meongcoach.community.domain.MentionTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MentionRequest(@NotBlank String targetType, @NotNull @Positive Long targetId) {

	public MentionCommand toCommand() {
		return new MentionCommand(MentionTargetType.from(targetType), targetId);
	}
}
