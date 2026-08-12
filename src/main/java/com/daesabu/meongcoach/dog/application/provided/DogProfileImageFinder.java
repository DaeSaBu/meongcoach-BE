package com.daesabu.meongcoach.dog.application.provided;

/**
 * 강아지 프로필 이미지 조회 능력.
 */
public interface DogProfileImageFinder {

	/**
	 * 강아지 한 마리의 프로필 이미지 URL을 조회한다. 이미지를 등록하지 않았으면 빈 문자열을 반환한다.
	 * 없거나 본인 소유가 아니면 {@code DogNotFoundException}을 던진다.
	 */
	String findProfileImageUrl(Long userId, Long dogId);
}
