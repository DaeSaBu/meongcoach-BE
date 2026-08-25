package com.daesabu.meongcoach.dog.domain;

import com.daesabu.meongcoach.dog.domain.exception.DogLimitExceededException;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.dog.domain.exception.LastDogNotDeletableException;
import java.util.List;

/**
 * 한 사용자가 소유한 (삭제되지 않은) 강아지 전체. 마리 수 상한, 마지막 강아지 삭제 금지, 사용자당 선택 한 마리처럼
 * 강아지 한 마리로는 판단할 수 없는 사용자 단위 규칙을 담는다.
 * 영속화 단위가 아니라 application이 조회한 목록으로 만드는 일급 컬렉션이며, 리포지토리를 알지 못한다.
 */
public class Dogs {

	// 온보딩 요청의 dogs 목록 크기 제한(OnboardingCompleteRequest)과 같은 값이다
	public static final int MAX_COUNT_PER_USER = 5;

	private final List<Dog> dogs;

	public Dogs(List<Dog> dogs) {
		this.dogs = List.copyOf(dogs);
	}

	/**
	 * 등록 가능하면 새 강아지를 만들어 반환한다. 저장은 호출자가 한다.
	 * 이미 {@value #MAX_COUNT_PER_USER}마리면 {@code DogLimitExceededException}을 던진다.
	 */
	public Dog register(DogRegisterCommand command) {
		if (dogs.size() >= MAX_COUNT_PER_USER) {
			throw new DogLimitExceededException();
		}
		Dog dog = Dog.register(command);
		// 사용자당 선택된 강아지는 한 마리다. 선택된 강아지가 없을 때 등록하는 강아지가 선택되며, 첫 등록이 여기 해당한다
		if (!hasSelected()) {
			dog.select();
		}
		return dog;
	}

	/**
	 * 강아지를 소프트 딜리트한다. 없거나 이 사용자 소유가 아니면 {@code DogNotFoundException},
	 * 마지막 한 마리면 {@code LastDogNotDeletableException}을 던진다.
	 * 소유 확인을 먼저 해 남의 강아지 요청에는 여전히 404가 나간다.
	 */
	public void delete(Long dogId) {
		Dog dog = find(dogId);
		// 강아지가 한 마리도 없는 상태를 막는다
		if (dogs.size() <= 1) {
			throw new LastDogNotDeletableException();
		}
		dog.delete();
	}

	private Dog find(Long dogId) {
		return dogs.stream()
				.filter(dog -> dog.getId().equals(dogId))
				.findFirst()
				.orElseThrow(() -> new DogNotFoundException(dogId));
	}

	private boolean hasSelected() {
		return dogs.stream().anyMatch(Dog::isSelected);
	}
}
