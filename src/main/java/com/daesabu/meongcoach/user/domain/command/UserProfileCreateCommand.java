package com.daesabu.meongcoach.user.domain.command;

import java.time.LocalDate;
import java.util.Set;

/**
 * 사용자 프로필 생성 입력. 모듈 경계를 넘는 값이라 mbti와 gender는 문자열 코드로 받고,
 * enum 변환·검증은 UserProfile 생성 시점에 일어난다.
 * birthDate와 profileImageUrl은 선택 입력이라 null을 허용하며, 선택하지 않은 교육 토픽도 null을 허용한다.
 */
public record UserProfileCreateCommand(String nickname, String profileImageUrl, LocalDate birthDate, String mbti,
                                       String gender, Set<Long> priorTrainingTopicIds,
                                       Set<Long> trainingGoalTopicIds) {
}
