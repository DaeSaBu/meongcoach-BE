package com.daesabu.meongcoach.dog.application;

import com.daesabu.meongcoach.dog.application.provided.DogRegister;
import com.daesabu.meongcoach.dog.application.provided.DogRegisterInfo;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Breed;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import com.daesabu.meongcoach.dog.domain.Personality;
import com.daesabu.meongcoach.dog.domain.exception.DogLimitExceededException;
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

	// 사용자당 등록할 수 있는 강아지 수. 온보딩 요청의 dogs 목록 크기 제한과 같은 값이다
	private static final int MAX_DOGS_PER_USER = 5;

	private final DogRepository dogRepository;

	@Override
	@Transactional
	public Long register(Long userId, DogRegisterInfo info) {
		// 소프트 딜리트된 강아지는 엔티티의 @SQLRestriction으로 개수에서 제외된다
		if (dogRepository.countByUserId(userId) >= MAX_DOGS_PER_USER) {
			throw new DogLimitExceededException();
		}
		Dog dog = Dog.register(new DogRegisterCommand(userId, info.name(), Breed.from(info.breed()),
				DogSex.from(info.sex()), info.birthDate(), info.weightKg(), info.profileImageUrl(), info.expectation()));
		dog.changePersonalities(Personality.fromCodes(info.personalities()));
		// 사용자당 선택된 강아지는 한 마리다. 선택된 강아지가 없을 때 등록하는 강아지가 선택되며, 첫 등록이 여기 해당한다
		if (!dogRepository.existsByUserIdAndStatus(userId, DogStatus.SELECTED)) {
			dog.select();
		}
		return dogRepository.save(dog).getId();
	}
}
