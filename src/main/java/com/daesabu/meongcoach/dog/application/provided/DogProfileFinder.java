package com.daesabu.meongcoach.dog.application.provided;

import com.daesabu.meongcoach.dog.domain.Dog;
import java.util.List;

/**
 * 강아지 프로필 조회 능력.
 */
public interface DogProfileFinder {

	/**
	 * 사용자가 보유한 강아지를 등록 순으로 모두 조회한다. 없으면 빈 리스트를 반환한다.
	 */
	List<Dog> findDogs(Long userId);
}
