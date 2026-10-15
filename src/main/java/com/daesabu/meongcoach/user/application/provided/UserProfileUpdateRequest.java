package com.daesabu.meongcoach.user.application.provided;

import com.daesabu.meongcoach.user.domain.UserProfileUpdateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UserProfileUpdateRequest(
		@NotBlank @Size(max = 50) String nickname,
		@Size(max = 512) String profileImageUrl,
		@Past LocalDate birthDate,
		@Size(max = 4) String mbti,
		@Size(max = 10) String gender) {

	public UserProfileUpdateCommand toCommand() {
		return new UserProfileUpdateCommand(nickname, profileImageUrl, birthDate, mbti, gender);
	}
}
