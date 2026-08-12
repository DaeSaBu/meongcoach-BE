package com.daesabu.meongcoach.dog.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.dog.domain.Breed;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.DogSex;
import java.math.BigDecimal;
import java.time.LocalDate;
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
	@DisplayName("강아지 ID와 소유자가 모두 일치할 때만 조회된다")
	void findByIdAndUserIdReturnsEmptyForOtherUsersDog() {
		Dog saved = persistDog(USER_ID);

		assertThat(dogRepository.findByIdAndUserId(saved.getId(), USER_ID)).isPresent();
		assertThat(dogRepository.findByIdAndUserId(saved.getId(), OTHER_USER_ID)).isEmpty();
	}

	private Dog persistDog(Long userId) {
		DogRegisterCommand command = new DogRegisterCommand(userId, "초코", Breed.POODLE, DogSex.MALE,
				LocalDate.of(2024, 3, 1), new BigDecimal("4.50"), IMAGE_URL, null);
		return dogRepository.saveAndFlush(Dog.register(command));
	}
}
