package com.daesabu.meongcoach.dog.application;

import com.daesabu.meongcoach.dog.application.provided.DogRegister;
import com.daesabu.meongcoach.dog.application.provided.DogRegisterInfo;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogProfileImage;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.Dogs;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.Personality;
import com.daesabu.meongcoach.media.application.provided.StoredMediaVerifier;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.dto.DogRegisterRequest;
import com.daesabu.meongcoach.dog.application.provided.dto.DogProfileUpdateRequest;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class DogRegisterService implements DogRegister {

	private final DogRepository dogRepository;
	private final DogProfileFinder dogProfileFinder;
	private final StoredMediaVerifier storedMediaVerifier;

	@Override
	@Transactional
	public Long register(Long userId, DogRegisterInfo info) {
		storedMediaVerifier.validateUrl(DogProfileImage.SCOPE, userId, info.profileImageUrl());
		List<Dog> owned = dogRepository.findAllByUserIdOrderByIdAsc(userId);
		Dogs dogs = new Dogs(owned);
		Dog dog = dogs.register(new DogRegisterCommand(userId, info.name(), Breed.from(info.breed()),
				DogSex.from(info.sex()), info.birthDate(), info.weightKg(), info.profileImageUrl(), info.expectation()));
		dog.changePersonalities(Personality.fromCodes(info.personalities()));
		return dogRepository.save(dog).getId();
	}
	@Override
	@Transactional
	public Long register(Long userId, DogRegisterRequest request) {
		DogRegisterInfo info = request.toInfo();
		return register(userId, info);
	}

	@Override
	@Transactional
	public Dog update(Long userId, Long dogId, DogProfileUpdateRequest request) {
		Dog dog = dogProfileFinder.findDog(userId, dogId);
		if (!Objects.equals(dog.getProfileImageUrl(), request.profileImageUrl())) {
			storedMediaVerifier.validateUrl(DogProfileImage.SCOPE, userId, request.profileImageUrl());
		}
		dog.updateProfile(request.toCommand());
		return dog;
	}
}

