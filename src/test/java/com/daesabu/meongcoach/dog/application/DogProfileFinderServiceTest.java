package com.daesabu.meongcoach.dog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Breed;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import com.daesabu.meongcoach.dog.domain.Personality;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(DogProfileFinderService.class)
@DisplayName("강아지 프로필 조회 서비스")
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
	@DisplayName("사용자의 모든 강아지를 등록 순으로 반환한다")
	void findDogsReturnsAllDogsInRegistrationOrder() {
		Dog selected = persistSelectedDog(USER_ID);
		Dog unselected = dogRepository.saveAndFlush(newDog(USER_ID));

		List<Dog> found = dogProfileFinder.findDogs(USER_ID);

		assertThat(found).extracting(Dog::getId).containsExactly(selected.getId(), unselected.getId());
	}

	@Test
	@DisplayName("강아지가 없으면 빈 리스트를 반환한다")
	void findDogsReturnsEmptyWhenNoDogExists() {
		assertThat(dogProfileFinder.findDogs(USER_ID)).isEmpty();
	}

	@Test
	@DisplayName("다른 사용자의 강아지는 반환하지 않는다")
	void findDogsIgnoresOtherUsersDog() {
		persistSelectedDog(OTHER_USER_ID);

		assertThat(dogProfileFinder.findDogs(USER_ID)).isEmpty();
	}

	@Test
	@DisplayName("사용자 소유 강아지를 ID로 조회해 프로필 정보를 반환한다")
	void findDogReturnsOwnedDogProfile() {
		Dog saved = persistSelectedDog(USER_ID);

		Dog found = dogProfileFinder.findDog(USER_ID, saved.getId());

		assertThat(found.getId()).isEqualTo(saved.getId());
		assertThat(found.getName()).isEqualTo("초코");
		assertThat(found.getBreed()).isEqualTo(Breed.POODLE);
		assertThat(found.getSex()).isEqualTo(DogSex.MALE);
		assertThat(found.getBirthDate()).isEqualTo(LocalDate.of(2024, 3, 1));
		assertThat(found.getWeightKg()).isEqualByComparingTo(new BigDecimal("4.50"));
		assertThat(found.getStatus()).isEqualTo(DogStatus.SELECTED);
		assertThat(found.getProfileImageUrl()).isEqualTo(IMAGE_URL);
		assertThat(found.getExpectation()).isEqualTo(EXPECTATION);
		assertThat(found.getPersonalities()).containsExactly(Personality.FRIENDLY);
	}

	@Test
	@DisplayName("다른 사용자의 강아지 ID로 조회하면 예외를 던진다")
	void findDogThrowsForOtherUsersDog() {
		Dog othersDog = persistSelectedDog(OTHER_USER_ID);

		assertThatThrownBy(() -> dogProfileFinder.findDog(USER_ID, othersDog.getId()))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	@DisplayName("없는 강아지 ID로 조회하면 예외를 던진다")
	void findDogThrowsWhenDogDoesNotExist() {
		assertThatThrownBy(() -> dogProfileFinder.findDog(USER_ID, 999L))
				.isInstanceOf(DogNotFoundException.class);
	}

	private Dog persistSelectedDog(Long userId) {
		Dog dog = newDog(userId);
		dog.select();
		return dogRepository.saveAndFlush(dog);
	}

	private Dog newDog(Long userId) {
		Dog dog = Dog.register(new DogRegisterCommand(userId, "초코", Breed.POODLE, DogSex.MALE,
				LocalDate.of(2024, 3, 1), new BigDecimal("4.50"), IMAGE_URL, EXPECTATION));
		dog.changePersonalities(Set.of(Personality.FRIENDLY));
		return dog;
	}
}
