package com.daesabu.meongcoach.dog.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 선택된 강아지의 프로필 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DogProfileFinderService implements DogProfileFinder {

	private final DogRepository dogRepository;

	@Override
	public Dog findSelectedDog(Long userId) {
		return dogRepository.findFirstByUserIdAndStatusOrderByIdAsc(userId, DogStatus.SELECTED)
				.orElseThrow(DogNotFoundException::new);
	}
}
