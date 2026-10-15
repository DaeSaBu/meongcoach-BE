package com.daesabu.meongcoach.dog.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileUpdater;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogProfileUpdateCommand;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DogProfileUpdateService implements DogProfileUpdater {

	private final DogRepository dogRepository;

	@Override
	@Transactional
	public Dog update(Long userId, Long dogId, DogProfileUpdateCommand command) {
		Dog dog = dogRepository.findByIdAndUserId(dogId, userId)
				.orElseThrow(() -> new DogNotFoundException(dogId));
		dog.updateProfile(command);
		return dog;
	}

	@Override
	@Transactional
	public void updateExpectation(Long userId, Long dogId, String expectation) {
		Dog dog = dogRepository.findByIdAndUserId(dogId, userId)
				.orElseThrow(() -> new DogNotFoundException(dogId));
		dog.changeExpectation(expectation);
	}
}
