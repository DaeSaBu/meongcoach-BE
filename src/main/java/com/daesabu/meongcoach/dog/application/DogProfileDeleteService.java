package com.daesabu.meongcoach.dog.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileDeleter;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소유 강아지를 소프트 딜리트한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DogProfileDeleteService implements DogProfileDeleter {

	private final DogRepository dogRepository;

	@Override
	@Transactional
	public void delete(Long userId, Long dogId) {
		// 소유자 필터를 겸한 조회라 남의 강아지는 존재하지 않는 것으로 처리되고, 이미 삭제된 강아지도 조회되지 않는다
		Dog dog = dogRepository.findByIdAndUserId(dogId, userId)
				.orElseThrow(() -> new DogNotFoundException(dogId));
		dog.delete();
		// 영속 상태 엔티티라 트랜잭션 커밋 시 변경이 반영된다
	}
}
