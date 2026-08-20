package com.daesabu.meongcoach.dog.application.provided;

/**
 * 강아지 프로필 이미지 조회 능력.
 */
public interface DogProfileImageFinder {

	/**
	 * 사용자가 선택한 강아지의 프로필 이미지를 조회한다. 이미지를 등록하지 않았으면 빈 문자열을 반환한다.
	 * 선택된 강아지가 없으면 {@code DogNotFoundException}을 던진다.
	 */
	DogProfileImageResult findSelectedProfileImage(Long userId);
}
