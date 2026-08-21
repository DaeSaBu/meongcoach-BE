package com.daesabu.meongcoach.dog.application.provided;

import com.daesabu.meongcoach.dog.domain.Dog;

/**
 * 강아지 프로필 조회 능력.
 */
public interface DogProfileFinder {

	/**
	 * 사용자가 선택한 강아지를 조회한다. 선택된 강아지가 없으면 {@code DogNotFoundException}을 던진다.
	 */
	Dog findSelectedDog(Long userId);
}
