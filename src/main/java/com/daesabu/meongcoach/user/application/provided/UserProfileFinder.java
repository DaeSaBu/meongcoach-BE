package com.daesabu.meongcoach.user.application.provided;

import com.daesabu.meongcoach.user.domain.UserProfile;
import java.util.Set;

/**
 * 사용자 프로필 조회 공개 API.
 */
public interface UserProfileFinder {

	/**
	 * 사용자의 프로필을 반환한다. 프로필이 없으면 {@code UserProfileNotFoundException}을 던진다.
	 */
	UserProfile findProfile(Long userId);

	/**
	 * 사용자가 이전에 교육한 토픽 ID 집합을 반환한다. 프로필이 없으면 {@code UserProfileNotFoundException}을 던진다.
	 */
	Set<Long> findPriorTrainingTopicIds(Long userId);
}
