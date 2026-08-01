package com.daesabu.meongcoach.onboarding.application;

import com.daesabu.meongcoach.dog.application.provided.DogRegister;
import com.daesabu.meongcoach.media.application.provided.StoredImageUrlValidator;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingCompleteInfo;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingCompleter;
import com.daesabu.meongcoach.user.application.provided.UserProfileCreateInfo;
import com.daesabu.meongcoach.user.application.provided.UserProfileRegister;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 온보딩 완료를 오케스트레이션한다. 입력에 포함된 외부 자원을 검증한 뒤,
 * 프로필과 강아지 생성을 하나의 트랜잭션으로 묶어 부분 성공이 남지 않게 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingCompleteService implements OnboardingCompleter {

	private final UserProfileRegister userProfileRegister;
	private final DogRegister dogRegister;
	private final StoredImageUrlValidator storedImageUrlValidator;

	@Override
	@Transactional
	public List<Long> complete(Long userId, OnboardingCompleteInfo info) {
		validateImageUrls(info);
		userProfileRegister.register(userId,
				new UserProfileCreateInfo(info.nickname(), info.birthDate(), info.mbti(), info.gender(),
						info.profileImageUrl(), info.priorTrainingTopicIds(), info.trainingGoalTopicIds()));
		return info.dogs().stream()
				.map(dog -> dogRegister.register(userId, dog))
				.toList();
	}

	// 업로드 완료 알림 없이 클라이언트가 보낸 URL을 믿는 방식이라,
	// 우리 스토리지 URL인지 저장 전에 검증한다
	private void validateImageUrls(OnboardingCompleteInfo info) {
		storedImageUrlValidator.validate(info.profileImageUrl());
		info.dogs().forEach(dog -> storedImageUrlValidator.validate(dog.profileImageUrl()));
	}
}
