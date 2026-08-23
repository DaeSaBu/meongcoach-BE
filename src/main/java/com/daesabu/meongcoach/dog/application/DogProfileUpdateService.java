package com.daesabu.meongcoach.dog.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileUpdater;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogProfileUpdateCommand;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소유 강아지의 프로필을 전체 교체한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DogProfileUpdateService implements DogProfileUpdater {

	private final DogRepository dogRepository;

	@Override
	@Transactional
	public Dog update(Long userId, Long dogId, DogProfileUpdateCommand command) {
		// 소유자 필터를 겸한 조회라 남의 강아지는 존재하지 않는 것으로 처리된다
		Dog dog = dogRepository.findByIdAndUserId(dogId, userId)
				.orElseThrow(() -> new DogNotFoundException(dogId));
		dog.updateProfile(command);
		// 영속 상태 엔티티라 트랜잭션 커밋 시 변경이 반영된다
		return dog;
	}
}
