package com.daesabu.meongcoach.onboarding.application.provided;

import com.daesabu.meongcoach.dog.domain.shared.DogRegisterCommand;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 온보딩 완료 입력. mbti와 gender는 필수이며, birthDate와 profileImageUrl은 선택 입력이라 null을 허용한다.
 * 선택하지 않은 교육 토픽은 빈 집합으로 전달한다.
 */
public record OnboardingCompleteInfo(String nickname, LocalDate birthDate, String mbti, String gender,
                                     String profileImageUrl, Set<Long> priorTrainingTopicIds,
                                     Set<Long> trainingGoalTopicIds, List<DogRegisterCommand> dogs) {
}
