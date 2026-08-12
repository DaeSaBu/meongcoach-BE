package com.daesabu.meongcoach.dog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.application.provided.DogProfileImageFinder;
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
@Import(DogProfileImageFinderService.class)
@DisplayName("강아지 프로필 이미지 조회 서비스")
class DogProfileImageFinderServiceTest {

	private static final Long USER_ID = 42L;
	private static final Long OTHER_USER_ID = 99L;
	private static final Long MISSING_DOG_ID = 999L;
	private static final String IMAGE_URL = "https://images.test.meongcoach.com/images/dog-profile/42/a.jpg";

	@Autowired
	private DogProfileImageFinder dogProfileImageFinder;

	@Autowired
	private DogRepository dogRepository;

	@Test
	@DisplayName("강아지의 프로필 이미지 URL을 반환한다")
	void findProfileImageUrlReturnsImageUrl() {
		Dog dog = persistDog(USER_ID, IMAGE_URL);

		String profileImageUrl = dogProfileImageFinder.findProfileImageUrl(USER_ID, dog.getId());

		assertThat(profileImageUrl).isEqualTo(IMAGE_URL);
	}

	@Test
	@DisplayName("프로필 이미지를 등록하지 않았으면 빈 문자열을 반환한다")
	void findProfileImageUrlReturnsEmptyStringWhenImageIsAbsent() {
		Dog dog = persistDog(USER_ID, null);

		String profileImageUrl = dogProfileImageFinder.findProfileImageUrl(USER_ID, dog.getId());

		assertThat(profileImageUrl).isEmpty();
	}

	@Test
	@DisplayName("다른 사용자의 강아지면 존재해도 예외를 던진다")
	void findProfileImageUrlThrowsForOtherUsersDog() {
		Dog dog = persistDog(OTHER_USER_ID, IMAGE_URL);

		assertThatThrownBy(() -> dogProfileImageFinder.findProfileImageUrl(USER_ID, dog.getId()))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	@DisplayName("존재하지 않는 강아지면 예외를 던진다")
	void findProfileImageUrlThrowsWhenDogDoesNotExist() {
		assertThatThrownBy(() -> dogProfileImageFinder.findProfileImageUrl(USER_ID, MISSING_DOG_ID))
				.isInstanceOf(DogNotFoundException.class);
	}

	private Dog persistDog(Long userId, String profileImageUrl) {
		DogRegisterCommand command = new DogRegisterCommand(userId, "초코", Breed.POODLE, DogSex.MALE,
				LocalDate.of(2024, 3, 1), new BigDecimal("4.50"), profileImageUrl, null);
		return dogRepository.saveAndFlush(Dog.register(command));
	}
}
