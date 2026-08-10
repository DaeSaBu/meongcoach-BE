package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.domain.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 프로필 조회를 담당한다. 프로필 행 부재(온보딩 미완료)는
 * 도메인의 "이미지 미설정 = 빈 문자열" 표현과 동일하게 빈 문자열로 접는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileFinderService implements UserProfileFinder {

	private final UserProfileRepository userProfileRepository;

	@Override
	public String findProfileImageUrl(Long userId) {
		return userProfileRepository.findById(userId)
				.map(UserProfile::getProfileImageUrl)
				.orElse("");
	}
}
