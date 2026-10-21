package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.community.domain.PostMediaCommand;
import com.daesabu.meongcoach.community.domain.PostMediaType;
import jakarta.validation.constraints.NotBlank;

public record PostMediaRequest(@NotBlank String type, @NotBlank String url) {

	public PostMediaCommand toCommand() {
		return new PostMediaCommand(PostMediaType.from(type), url);
	}
}
