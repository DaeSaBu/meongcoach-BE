package com.daesabu.meongcoach.user.application.provided;

import com.daesabu.meongcoach.user.domain.UserProfile;
import jakarta.validation.Valid;
import java.util.Collection;

/**
 * 사용자 프로필 수정 공개 API.
 */
public interface UserProfileUpdater {

	/**
	 * 프로필의 입력 필드를 요청 값으로 모두 교체하고 수정된 프로필을 반환한다.
	 * 이미지 URL이 우리 스토리지 URL이 아니거나 MBTI·성별 값이 올바르지 않으면 예외를 던지며,
	 * 프로필이 없으면 {@code UserProfileNotFoundException}을 던진다.
	 */
	UserProfile update(Long userId, @Valid UserProfileUpdateRequest request);

	/**
	 * 이전에 교육한 토픽 ID를 기존 값에 누적한다. 중복은 한 번만 저장하며,
	 * 프로필이 없으면 {@code UserProfileNotFoundException}을 던진다.
	 */
	void addPriorTrainingTopics(Long userId, Collection<Long> topicIds);
}
