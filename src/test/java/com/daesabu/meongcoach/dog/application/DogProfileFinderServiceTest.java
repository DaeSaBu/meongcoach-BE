package com.daesabu.meongcoach.dog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Breed;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(DogProfileFinderService.class)
@DisplayName("선택된 강아지 프로필 조회 서비스")
class DogProfileFinderServiceTest {

	private static final Long USER_ID = 42L;
	private static final Long OTHER_USER_ID = 99L;
	private static final String IMAGE_URL = "https://images.test.meongcoach.com/images/dog-profile/42/a.jpg";
	private static final String EXPECTATION = "보호자와 즐겁게 교육받고 싶어요.";

	@Autowired
	private DogProfileFinder dogProfileFinder;

	@Autowired
	private DogRepository dogRepository;

	@Test
	@DisplayName("선택된 강아지의 프로필 정보를 반환한다")
	void findSelectedDogReturnsProfile() {
		Dog saved = persistSelectedDog(USER_ID);

		Dog found = dogProfileFinder.findSelectedDog(USER_ID);

		assertThat(found.getId()).isEqualTo(saved.getId());
		assertThat(found.getName()).isEqualTo("초코");
		assertThat(found.getBreed()).isEqualTo(Breed.POODLE);
		assertThat(found.getSex()).isEqualTo(DogSex.MALE);
		assertThat(found.getBirthDate()).isEqualTo(LocalDate.of(2024, 3, 1));
		assertThat(found.getWeightKg()).isEqualByComparingTo(new BigDecimal("4.50"));
		assertThat(found.getProfileImageUrl()).isEqualTo(IMAGE_URL);
		assertThat(found.getExpectation()).isEqualTo(EXPECTATION);
	}

	@Test
	@DisplayName("미선택 강아지와 함께 있어도 선택된 강아지를 반환한다")
	void findSelectedDogIgnoresUnselectedDog() {
		dogRepository.saveAndFlush(newDog(USER_ID));
		Dog selected = persistSelectedDog(USER_ID);

		Dog found = dogProfileFinder.findSelectedDog(USER_ID);

		assertThat(found.getId()).isEqualTo(selected.getId());
	}

	@Test
	@DisplayName("미선택 강아지만 있으면 예외를 던진다")
	void findSelectedDogThrowsWhenOnlyUnselectedDogExists() {
		dogRepository.saveAndFlush(newDog(USER_ID));

		assertThatThrownBy(() -> dogProfileFinder.findSelectedDog(USER_ID))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	@DisplayName("강아지가 한 마리도 없으면 예외를 던진다")
	void findSelectedDogThrowsWhenNoDogExists() {
		assertThatThrownBy(() -> dogProfileFinder.findSelectedDog(USER_ID))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	@DisplayName("다른 사용자의 선택된 강아지는 반환하지 않는다")
	void findSelectedDogThrowsForOtherUsersDog() {
		persistSelectedDog(OTHER_USER_ID);

		assertThatThrownBy(() -> dogProfileFinder.findSelectedDog(USER_ID))
				.isInstanceOf(DogNotFoundException.class);
	}

	private Dog persistSelectedDog(Long userId) {
		Dog dog = newDog(userId);
		dog.select();
		return dogRepository.saveAndFlush(dog);
	}

	private Dog newDog(Long userId) {
		return Dog.register(new DogRegisterCommand(userId, "초코", Breed.POODLE, DogSex.MALE,
				LocalDate.of(2024, 3, 1), new BigDecimal("4.50"), IMAGE_URL, EXPECTATION));
	}
}
