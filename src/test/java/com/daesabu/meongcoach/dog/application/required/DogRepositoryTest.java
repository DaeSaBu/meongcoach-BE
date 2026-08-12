package com.daesabu.meongcoach.dog.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.dog.domain.Breed;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/**
 * 강아지 리포지토리 검증.
 */
@DataJpaTest
@DisplayName("강아지 리포지토리")
class DogRepositoryTest {

	private static final Long USER_ID = 42L;
	private static final Long OTHER_USER_ID = 99L;
	private static final String IMAGE_URL = "https://images.test.meongcoach.com/images/dog-profile/42/a.jpg";

	@Autowired
	private DogRepository dogRepository;

	@Test
	@DisplayName("선택된 강아지가 있으면 존재한다고 알려준다")
	void existsByUserIdAndStatusReturnsTrueWhenSelectedDogExists() {
		persistSelectedDog(USER_ID);

		assertThat(dogRepository.existsByUserIdAndStatus(USER_ID, DogStatus.SELECTED)).isTrue();
	}

	@Test
	@DisplayName("미선택 강아지만 있으면 선택된 강아지가 없다고 알려준다")
	void existsByUserIdAndStatusReturnsFalseWhenOnlyUnselectedDogExists() {
		persistUnselectedDog(USER_ID);

		assertThat(dogRepository.existsByUserIdAndStatus(USER_ID, DogStatus.SELECTED)).isFalse();
	}

	@Test
	@DisplayName("다른 사용자의 선택된 강아지는 세지 않는다")
	void existsByUserIdAndStatusIgnoresOtherUsersDog() {
		persistSelectedDog(OTHER_USER_ID);

		assertThat(dogRepository.existsByUserIdAndStatus(USER_ID, DogStatus.SELECTED)).isFalse();
	}

	@Test
	@DisplayName("사용자의 선택된 강아지를 조회한다")
	void findFirstByUserIdAndStatusReturnsSelectedDog() {
		persistUnselectedDog(USER_ID);
		Dog selected = persistSelectedDog(USER_ID);

		Optional<Dog> found = dogRepository.findFirstByUserIdAndStatusOrderByIdAsc(USER_ID, DogStatus.SELECTED);

		assertThat(found.orElseThrow().getId()).isEqualTo(selected.getId());
	}

	// 애플리케이션 레이어에서만 불변식을 강제하므로, 선택이 여러 건인 데이터가 남아 있어도 예외 없이 끝나야 한다
	@Test
	@DisplayName("선택된 강아지가 여러 건이어도 가장 먼저 등록된 한 마리를 반환한다")
	void findFirstByUserIdAndStatusReturnsEarliestWhenSelectionIsDuplicated() {
		Dog first = persistSelectedDog(USER_ID);
		persistSelectedDog(USER_ID);

		Optional<Dog> found = dogRepository.findFirstByUserIdAndStatusOrderByIdAsc(USER_ID, DogStatus.SELECTED);

		assertThat(found.orElseThrow().getId()).isEqualTo(first.getId());
	}

	@Test
	@DisplayName("선택된 강아지가 없으면 빈 Optional을 반환한다")
	void findFirstByUserIdAndStatusReturnsEmptyWhenNoSelectedDogExists() {
		persistUnselectedDog(USER_ID);

		assertThat(dogRepository.findFirstByUserIdAndStatusOrderByIdAsc(USER_ID, DogStatus.SELECTED)).isEmpty();
	}

	private Dog persistSelectedDog(Long userId) {
		Dog dog = newDog(userId);
		dog.select();
		return dogRepository.saveAndFlush(dog);
	}

	private Dog persistUnselectedDog(Long userId) {
		return dogRepository.saveAndFlush(newDog(userId));
	}

	private Dog newDog(Long userId) {
		return Dog.register(new DogRegisterCommand(userId, "초코", Breed.POODLE, DogSex.MALE,
				LocalDate.of(2024, 3, 1), new BigDecimal("4.50"), IMAGE_URL, null));
	}
}
