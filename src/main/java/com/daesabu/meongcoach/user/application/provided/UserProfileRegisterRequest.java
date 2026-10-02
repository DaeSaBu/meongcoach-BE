package com.daesabu.meongcoach.user.application.provided;

import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import java.time.LocalDate;
import java.util.Set;

/**
 * 사용자 프로필 등록 입력. 모듈 경계를 넘는 값이라 mbti와 gender는 문자열 코드로 받고, 변환·검증은 user 모듈이 수행한다.
 * birthDate와 profileImageUrl은 선택 입력이라 null을 허용하며, 선택하지 않은 교육 토픽도 null을 허용한다.
 */
public record UserProfileRegisterRequest(String nickname, String profileImageUrl, LocalDate birthDate, String mbti,
                                         String gender, Set<Long> priorTrainingTopicIds,
                                         Set<Long> trainingGoalTopicIds) {

	public UserProfileCreateCommand toCommand() {
		return new UserProfileCreateCommand(nickname, profileImageUrl, birthDate, mbti, gender, priorTrainingTopicIds,
				trainingGoalTopicIds);
	}
}
