package com.daesabu.meongcoach.dog.application;

import com.daesabu.meongcoach.dog.application.provided.DogRegister;
import com.daesabu.meongcoach.dog.application.provided.DogRegisterInfo;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Breed;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.Personality;
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
		Dog dog = Dog.register(new DogRegisterCommand(userId, info.name(), Breed.from(info.breed()),
				DogSex.from(info.sex()), info.birthDate(), info.weightKg()));
		dog.changePersonalities(parsePersonalities(info.personalities()));
		// null은 changeProfileImage가 미설정(빈 문자열)으로 정규화한다
		dog.changeProfileImage(info.profileImageUrl());
		// null은 changeExpectation이 미설정(빈 문자열)으로 정규화한다
		dog.changeExpectation(info.expectation());
		return dogRepository.save(dog).getId();
	}

	// null은 '성격 미선택'이므로 빈 Set으로 취급한다
	private Set<Personality> parsePersonalities(Set<String> personalities) {
		if (personalities == null) {
			return Set.of();
		}
		return personalities.stream()
				.map(Personality::from)
				.collect(Collectors.toSet());
	}
}
