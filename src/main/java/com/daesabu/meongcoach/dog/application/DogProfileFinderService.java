package com.daesabu.meongcoach.dog.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.DogProfileResult;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.dog.domain.shared.Personality;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DogProfileFinderService implements DogProfileFinder {

	private final DogRepository dogRepository;
	private final Clock clock;

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
	public DogProfileResult findProfile(Long userId, Long dogId) {
		Dog dog = findDog(userId, dogId);
		return toProfile(dog);
	}

	@Override
	public Optional<DogProfileResult> findProfileIfPresent(Long userId, Long dogId) {
		return dogRepository.findByIdAndUserId(dogId, userId)
				.map(this::toProfile);
	}

	private DogProfileResult toProfile(Dog dog) {
		List<String> personalityLabels = dog.getPersonalities().stream()
				.sorted()
				.map(Personality::getLabel)
				.toList();
		return new DogProfileResult(dog.getId(), dog.getName(), dog.getBirthDate(), dog.isBirthDateEstimated(),
				dog.getAgeInMonths(LocalDate.now(clock)), dog.getBreed().getLabel(), dog.getSex().name(), personalityLabels,
				dog.getWeightKg(), dog.getExpectation());
	}
}
