package com.daesabu.meongcoach.dog.application;

import com.daesabu.meongcoach.dog.application.provided.DogRegister;
import com.daesabu.meongcoach.dog.application.provided.DogRegisterInfo;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.Personality;
import com.daesabu.meongcoach.dog.domain.exception.InvalidDogSexException;
import com.daesabu.meongcoach.dog.domain.exception.InvalidPersonalityException;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문자열 입력을 도메인 타입으로 변환·검증한 뒤 강아지를 등록한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DogRegisterService implements DogRegister {

	private final DogRepository dogRepository;

	@Override
	@Transactional
	public Long register(Long userId, DogRegisterInfo info) {
		Dog dog = Dog.register(new DogRegisterCommand(userId, info.name(), info.breed(),
				parseSex(info.sex()), info.birthDate(), info.weightKg()));
		dog.changePersonalities(parsePersonalities(info.personalities()));
		return dogRepository.save(dog).getId();
	}

	private DogSex parseSex(String sex) {
		if (sex == null) {
			throw new InvalidDogSexException(null);
		}
		try {
			return DogSex.valueOf(sex);
		} catch (IllegalArgumentException e) {
			throw new InvalidDogSexException(sex);
		}
	}

	private Set<Personality> parsePersonalities(Set<String> personalities) {
		if (personalities == null) {
			return Set.of();
		}
		return personalities.stream()
				.map(this::parsePersonality)
				.collect(Collectors.toSet());
	}

	private Personality parsePersonality(String personality) {
		try {
			return Personality.valueOf(personality);
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new InvalidPersonalityException(personality);
		}
	}
}
