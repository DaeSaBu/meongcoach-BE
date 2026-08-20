package com.daesabu.meongcoach.dog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.application.provided.DogProfileImageFinder;
import com.daesabu.meongcoach.dog.application.provided.DogProfileImageResult;
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
@DisplayName("선택된 강아지 프로필 이미지 조회 서비스")
class DogProfileImageFinderServiceTest {

	private static final Long USER_ID = 42L;
	private static final Long OTHER_USER_ID = 99L;
	private static final String IMAGE_URL = "https://images.test.meongcoach.com/images/dog-profile/42/a.jpg";

	@Autowired
	private DogProfileImageFinder dogProfileImageFinder;

	@Autowired
	private DogRepository dogRepository;

	@Test
	@DisplayName("선택된 강아지의 id와 프로필 이미지 URL을 반환한다")
	void findSelectedProfileImageReturnsImageUrl() {
		Dog dog = persistSelectedDog(USER_ID, IMAGE_URL);

		DogProfileImageResult profileImage = dogProfileImageFinder.findSelectedProfileImage(USER_ID);

		assertThat(profileImage.id()).isEqualTo(dog.getId());
		assertThat(profileImage.profileImageUrl()).isEqualTo(IMAGE_URL);
	}

	@Test
	@DisplayName("프로필 이미지를 등록하지 않았으면 빈 문자열을 반환한다")
	void findSelectedProfileImageReturnsEmptyStringWhenImageIsAbsent() {
		persistSelectedDog(USER_ID, null);

		DogProfileImageResult profileImage = dogProfileImageFinder.findSelectedProfileImage(USER_ID);

		assertThat(profileImage.profileImageUrl()).isEmpty();
	}

	@Test
	@DisplayName("미선택 강아지만 있으면 예외를 던진다")
	void findSelectedProfileImageThrowsWhenOnlyUnselectedDogExists() {
		dogRepository.saveAndFlush(newDog(USER_ID, IMAGE_URL));

		assertThatThrownBy(() -> dogProfileImageFinder.findSelectedProfileImage(USER_ID))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	@DisplayName("강아지가 한 마리도 없으면 예외를 던진다")
	void findSelectedProfileImageThrowsWhenNoDogExists() {
		assertThatThrownBy(() -> dogProfileImageFinder.findSelectedProfileImage(USER_ID))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	@DisplayName("다른 사용자의 선택된 강아지는 반환하지 않는다")
	void findSelectedProfileImageThrowsForOtherUsersDog() {
		persistSelectedDog(OTHER_USER_ID, IMAGE_URL);

		assertThatThrownBy(() -> dogProfileImageFinder.findSelectedProfileImage(USER_ID))
				.isInstanceOf(DogNotFoundException.class);
	}

	private Dog persistSelectedDog(Long userId, String profileImageUrl) {
		Dog dog = newDog(userId, profileImageUrl);
		dog.select();
		return dogRepository.saveAndFlush(dog);
	}

	private Dog newDog(Long userId, String profileImageUrl) {
		return Dog.register(new DogRegisterCommand(userId, "초코", Breed.POODLE, DogSex.MALE,
				LocalDate.of(2024, 3, 1), new BigDecimal("4.50"), profileImageUrl, null));
	}
}
