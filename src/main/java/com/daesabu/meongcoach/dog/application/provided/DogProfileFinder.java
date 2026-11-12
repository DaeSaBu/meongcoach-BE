package com.daesabu.meongcoach.dog.application.provided;

import com.daesabu.meongcoach.dog.application.provided.dto.DogProfileResult;
import com.daesabu.meongcoach.dog.domain.Dog;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 강아지 프로필 조회 능력.
 */
public interface DogProfileFinder {

	/**
	 * 사용자가 보유한 강아지를 등록 순으로 모두 조회한다. 없으면 빈 리스트를 반환한다.
	 */
	List<Dog> findDogs(Long userId);

	/**
	 * 사용자가 소유한 강아지 한 마리를 ID로 조회한다.
	 * 없거나 본인 소유가 아니면 {@code DogNotFoundException}을 던진다.
	 */
	Dog findDog(Long userId, Long dogId);

	/**
	 * 사용자가 선택한 강아지를 조회한다. 선택된 강아지가 없으면 {@code DogNotFoundException}을 던진다.
	 */
	Dog findSelectedDog(Long userId);

	/**
	 * 사용자가 선택한 강아지의 ID, 이름, 품종, 생년월일, 몸무게를 조회한다.
	 * 선택된 강아지가 없으면 {@code DogNotFoundException}을 던진다.
	 */
	DogProfileResult findSelectedDogProfile(Long userId);

	/**
	 * 사용자가 선택한 강아지의 공개 정보(이름, 품종, 성별, 나이, 프로필 이미지)를 조회한다.
	 * 선택된 강아지가 없으면 빈 값을 반환한다.
	 */
	Optional<PublicDogResult> findSelectedPublicDog(Long userId);

	/**
	 * 사용자마다 선택된 강아지 하나의 이름과 프로필 이미지를 반환한다.
	 * 삭제된 강아지는 제외하며, 선택된 강아지가 없는 사용자는 결과에서 빠진다.
	 */
	List<SelectedDogResult> findSelectedDogs(Collection<Long> userIds);

	/**
	 * 지정한 강아지의 프로필을 반환한다. 없거나 본인 소유가 아니면 DogNotFoundException을 던진다.
	 */
	DogProfileResult findDogProfile(Long userId, Long dogId);
}
