package com.daesabu.meongcoach.user.adapter.webapi.dto;

import com.daesabu.meongcoach.user.domain.UserProfile;
import java.time.LocalDate;
import java.util.List;

public record UserProfileResponse(String nickname, String profileImageUrl, LocalDate birthDate, String mbti,
                                  String gender, List<Long> priorTrainingTopicIds) {

	public static UserProfileResponse from(UserProfile profile) {
		return new UserProfileResponse(
				profile.getNickname(),
				profile.getProfileImageUrl(),
				profile.getBirthDate(),
				codeOf(profile.getMbti()),
				codeOf(profile.getGender()),
				profile.getPriorTrainingTopicIds().stream().sorted().toList());
	}

	private static String codeOf(Enum<?> value) {
		if (value == null) {
			return null;
		}
		return value.name();
	}
}
