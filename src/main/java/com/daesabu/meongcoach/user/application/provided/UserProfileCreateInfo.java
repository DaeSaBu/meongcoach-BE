package com.daesabu.meongcoach.user.application.provided;

import java.time.LocalDate;
import java.util.Set;

/**
 * 사용자 프로필 생성 입력. 모듈 경계를 넘는 값이라 enum 대신 문자열을 받고, 변환·검증은 user 모듈이 수행한다.
 * mbti와 gender는 필수이며, birthDate와 profileImageUrl은 선택 입력이라 null을 허용한다.
 */
public record UserProfileCreateInfo(String nickname, LocalDate birthDate, String mbti, String gender,
                                    String profileImageUrl, Set<Long> priorTrainingTopicIds,
                                    Set<Long> trainingGoalTopicIds) {

	public UserProfileCreateInfo(String nickname, LocalDate birthDate, String mbti, String gender,
	                             String profileImageUrl) {
		this(nickname, birthDate, mbti, gender, profileImageUrl, Set.of(), Set.of());
	}
}
