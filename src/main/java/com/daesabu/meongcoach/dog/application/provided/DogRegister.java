package com.daesabu.meongcoach.dog.application.provided;

/**
 * 강아지 등록 공개 API.
 */
public interface DogRegister {

	/**
	 * 강아지를 등록하고 생성된 ID를 반환한다.
	 * 사용자의 강아지가 이미 5마리면 {@code DogLimitExceededException}을 던지고 등록하지 않는다.
	 * 프로필 이미지가 우리 스토리지의 URL이 아니면 {@code InvalidImageUrlException}을 던진다.
	 */
	Long register(Long userId, DogRegisterInfo info);
}
