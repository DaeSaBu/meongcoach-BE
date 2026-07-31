package com.daesabu.meongcoach.dog.application.provided;

/**
 * 강아지 등록 공개 API.
 */
public interface DogRegister {

	/**
	 * 강아지를 등록하고 생성된 ID를 반환한다.
	 */
	Long register(Long userId, DogRegisterInfo info);
}
