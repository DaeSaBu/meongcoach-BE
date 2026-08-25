package com.daesabu.meongcoach.dog.application;

import com.daesabu.meongcoach.dog.application.provided.DogRegister;
import com.daesabu.meongcoach.dog.application.provided.DogRegisterInfo;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Breed;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.Dogs;
import com.daesabu.meongcoach.dog.domain.Personality;
import java.util.List;
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
		// 마리 수 상한·선택 여부는 사용자 단위 규칙이라 Dogs가 판단한다.
		// 소프트 딜리트된 강아지는 엔티티의 @SQLRestriction으로 조회에서 제외되므로 살아있는 강아지만 담긴다
		List<Dog> owned = dogRepository.findAllByUserIdOrderByIdAsc(userId);
		Dogs dogs = new Dogs(owned);
		Dog dog = dogs.register(new DogRegisterCommand(userId, info.name(), Breed.from(info.breed()),
				DogSex.from(info.sex()), info.birthDate(), info.weightKg(), info.profileImageUrl(), info.expectation()));
		dog.changePersonalities(Personality.fromCodes(info.personalities()));
		return dogRepository.save(dog).getId();
	}
}
