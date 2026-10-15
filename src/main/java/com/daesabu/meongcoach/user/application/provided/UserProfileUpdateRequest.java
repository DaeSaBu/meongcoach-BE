package com.daesabu.meongcoach.user.application.provided;

import com.daesabu.meongcoach.user.domain.UserProfileUpdateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
		@NotBlank @Size(max = 50) String nickname,
		@Size(max = 512) String profileImageUrl,
		@Size(max = 4) String mbti) {

	public UserProfileUpdateCommand toCommand() {
		return new UserProfileUpdateCommand(nickname, profileImageUrl, mbti);
	}
}
