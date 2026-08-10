package com.daesabu.meongcoach.user.application.provided;

/**
 * 사용자 프로필 조회 공개 API.
 */
public interface UserProfileFinder {

	/**
	 * 사용자의 프로필 이미지 URL을 반환한다. 프로필이 없거나(온보딩 미완료) 이미지 미설정이면 빈 문자열을 반환한다.
	 */
	String findProfileImageUrl(Long userId);
}
