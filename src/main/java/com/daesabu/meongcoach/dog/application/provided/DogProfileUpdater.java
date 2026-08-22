package com.daesabu.meongcoach.dog.application.provided;

import com.daesabu.meongcoach.dog.domain.Dog;

/**
 * 강아지 프로필 수정 능력.
 */
public interface DogProfileUpdater {

	/**
	 * 사용자가 소유한 강아지의 프로필을 입력값으로 전체 교체하고 수정된 강아지를 반환한다.
	 * 없거나 본인 소유가 아니면 {@code DogNotFoundException}을 던진다.
	 */
	Dog update(Long userId, Long dogId, DogProfileUpdateInfo info);
}
