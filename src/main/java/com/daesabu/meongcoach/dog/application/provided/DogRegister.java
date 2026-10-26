package com.daesabu.meongcoach.dog.application.provided;

import com.daesabu.meongcoach.dog.application.provided.dto.DogRegisterRequest;
import com.daesabu.meongcoach.dog.application.provided.dto.DogProfileUpdateRequest;
import com.daesabu.meongcoach.dog.domain.Dog;
import jakarta.validation.Valid;

/**
 * 강아지 등록 공개 API.
 */
public interface DogRegister {

	/**
	 * 강아지를 등록하고 생성된 ID를 반환한다.
	 * 사용자의 강아지가 이미 5마리면 {@code DogLimitExceededException}을 던지고 등록하지 않는다.
	 * 프로필 이미지가 사용자가 강아지 프로필 경로에 올린 우리 스토리지의 URL이 아니면 {@code InvalidMediaUrlException}을 던진다.
	 */
	Long register(Long userId, DogRegisterInfo info);
	/**
	 * 입력을 검증해 강아지를 등록하고 ID를 반환한다.
	 */
	Long register(Long userId, @Valid DogRegisterRequest request);

	/**
	 * 소유한 강아지의 프로필을 교체한다. 기존 사진 URL은 유지할 수 있고, 바뀐 사진은 본인 소유 URL인지 검증한다.
	 * 강아지가 없거나 본인 소유가 아니면 DogNotFoundException을 던진다.
	 */
	Dog update(Long userId, Long dogId, @Valid DogProfileUpdateRequest request);
}

