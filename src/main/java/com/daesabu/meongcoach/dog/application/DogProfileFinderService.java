package com.daesabu.meongcoach.dog.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.SelectedDogResult;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 강아지 프로필 조회 서비스. 보유 강아지 목록, 소유 강아지 단건, 선택된 강아지를 조회한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DogProfileFinderService implements DogProfileFinder {

	private final DogRepository dogRepository;

	@Override
	public List<Dog> findDogs(Long userId) {
		return dogRepository.findAllByUserIdOrderByIdAsc(userId);
	}

	@Override
	public Dog findDog(Long userId, Long dogId) {
		return dogRepository.findByIdAndUserId(dogId, userId)
				.orElseThrow(() -> new DogNotFoundException(dogId));
	}

	@Override
	public Dog findSelectedDog(Long userId) {
		return dogRepository.findFirstByUserIdAndStatusOrderByIdAsc(userId, DogStatus.SELECTED)
				.orElseThrow(DogNotFoundException::new);
	}

	@Override
	public List<SelectedDogResult> findSelectedDogs(Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return List.of();
		}
		List<Dog> dogs = dogRepository.findAllByUserIdInAndStatusOrderByIdAsc(userIds, DogStatus.SELECTED);
		Map<Long, Dog> selectedDogs = dogs.stream()
				.collect(Collectors.toMap(Dog::getUserId, Function.identity(), (first, later) -> first,
						LinkedHashMap::new));
		return selectedDogs.values().stream()
				.map(SelectedDogResult::from)
				.toList();
	}
}
