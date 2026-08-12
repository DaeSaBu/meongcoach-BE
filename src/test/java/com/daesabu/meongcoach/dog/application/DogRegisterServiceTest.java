package com.daesabu.meongcoach.dog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.application.provided.DogRegisterInfo;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Breed;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import com.daesabu.meongcoach.dog.domain.Personality;
import com.daesabu.meongcoach.dog.domain.exception.InvalidBreedException;
import com.daesabu.meongcoach.dog.domain.exception.InvalidDogSexException;
import com.daesabu.meongcoach.dog.domain.exception.InvalidPersonalityException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
@DisplayName("강아지 등록 서비스")
class DogRegisterServiceTest {

	@Autowired
	private DogRepository dogRepository;

	private DogRegisterService service;

	@BeforeEach
	void setUp() {
		service = new DogRegisterService(dogRepository);
	}

	private DogRegisterInfo registerInfo(String sex, Set<String> personalities) {
		return new DogRegisterInfo("초코", "POODLE", sex, LocalDate.of(2024, 3, 1),
				new BigDecimal("4.50"), personalities, null, null);
	}

	@Test
	@DisplayName("강아지를 등록하면 성격과 함께 저장된다")
	void registerSavesDogWithPersonalities() {
		Long dogId = service.register(1L, registerInfo("MALE", Set.of("TIMID", "LIVELY")));

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getUserId()).isEqualTo(1L);
		assertThat(dog.getName()).isEqualTo("초코");
		assertThat(dog.getBreed()).isEqualTo(Breed.POODLE);
		assertThat(dog.getSex()).isEqualTo(DogSex.MALE);
		assertThat(dog.getStatus()).isEqualTo(DogStatus.SELECTED);
		assertThat(dog.getPersonalities()).containsExactlyInAnyOrder(Personality.TIMID, Personality.LIVELY);
	}

	@Test
	@DisplayName("두 번째로 등록한 강아지는 미선택 상태가 된다")
	void registerLeavesSecondDogUnselected() {
		Long firstDogId = service.register(1L, registerInfo("MALE", Set.of()));

		Long secondDogId = service.register(1L, registerInfo("FEMALE", Set.of()));

		Dog firstDog = dogRepository.findById(firstDogId).orElseThrow();
		Dog secondDog = dogRepository.findById(secondDogId).orElseThrow();
		assertThat(firstDog.getStatus()).isEqualTo(DogStatus.SELECTED);
		assertThat(secondDog.getStatus()).isEqualTo(DogStatus.UNSELECTED);
	}

	@Test
	@DisplayName("다른 사용자의 선택된 강아지는 선택 여부에 영향을 주지 않는다")
	void registerSelectsFirstDogOfEachUser() {
		service.register(1L, registerInfo("MALE", Set.of()));

		Long otherUserDogId = service.register(2L, registerInfo("MALE", Set.of()));

		Dog otherUserDog = dogRepository.findById(otherUserDogId).orElseThrow();
		assertThat(otherUserDog.getStatus()).isEqualTo(DogStatus.SELECTED);
	}

	@Test
	@DisplayName("성격이 없으면 빈 성격으로 등록된다")
	void registerAllowsNullPersonalities() {
		Long dogId = service.register(1L, registerInfo("FEMALE", null));

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getPersonalities()).isEmpty();
	}

	@Test
	@DisplayName("생년월일이 없어도 등록할 수 있다")
	void registerAllowsNullBirthDate() {
		DogRegisterInfo info = new DogRegisterInfo("초코", "POODLE", "MALE", null,
				new BigDecimal("4.50"), Set.of(), null, null);

		Long dogId = service.register(1L, info);

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getBirthDate()).isNull();
	}

	@Test
	@DisplayName("프로필 이미지 URL을 함께 저장한다")
	void registerSavesProfileImage() {
		String imageUrl = "https://images.test.meongcoach.com/images/dog-profile/1/a.jpg";
		DogRegisterInfo info = new DogRegisterInfo("초코", "POODLE", "MALE", null,
				new BigDecimal("4.50"), Set.of(), imageUrl, null);

		Long dogId = service.register(1L, info);

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getProfileImageUrl()).isEqualTo(imageUrl);
	}

	@Test
	@DisplayName("프로필 이미지가 없으면 빈 문자열로 저장한다")
	void registerStoresEmptyProfileImageWhenAbsent() {
		Long dogId = service.register(1L, registerInfo("MALE", Set.of()));

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getProfileImageUrl()).isEmpty();
	}

	@Test
	@DisplayName("기대 사항을 함께 저장한다")
	void registerSavesExpectation() {
		DogRegisterInfo info = new DogRegisterInfo("초코", "POODLE", "MALE", null,
				new BigDecimal("4.50"), Set.of(), null, "보호자와 즐겁게 교육받고 싶어요.");

		Long dogId = service.register(1L, info);

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getExpectation()).isEqualTo("보호자와 즐겁게 교육받고 싶어요.");
	}

	@Test
	@DisplayName("기대 사항이 없으면 빈 문자열로 저장한다")
	void registerStoresEmptyExpectationWhenAbsent() {
		Long dogId = service.register(1L, registerInfo("MALE", Set.of()));

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getExpectation()).isEmpty();
	}

	@Test
	@DisplayName("잘못된 견종 값이면 등록에 실패한다")
	void registerFailsWhenBreedIsInvalid() {
		DogRegisterInfo info = new DogRegisterInfo("초코", "UNKNOWN", "MALE", null,
				new BigDecimal("4.50"), Set.of(), null, null);

		assertThatThrownBy(() -> service.register(1L, info))
				.isInstanceOf(InvalidBreedException.class);
	}

	@Test
	@DisplayName("견종 값이 없으면 등록에 실패한다")
	void registerFailsWhenBreedIsNull() {
		DogRegisterInfo info = new DogRegisterInfo("초코", null, "MALE", null,
				new BigDecimal("4.50"), Set.of(), null, null);

		assertThatThrownBy(() -> service.register(1L, info))
				.isInstanceOf(InvalidBreedException.class);
	}

	@Test
	@DisplayName("잘못된 성별 값이면 등록에 실패한다")
	void registerFailsWhenSexIsInvalid() {
		assertThatThrownBy(() -> service.register(1L, registerInfo("UNKNOWN", Set.of())))
				.isInstanceOf(InvalidDogSexException.class);
	}

	@Test
	@DisplayName("성별 값이 없으면 등록에 실패한다")
	void registerFailsWhenSexIsNull() {
		assertThatThrownBy(() -> service.register(1L, registerInfo(null, Set.of())))
				.isInstanceOf(InvalidDogSexException.class);
	}

	@Test
	@DisplayName("잘못된 성격 값이면 등록에 실패한다")
	void registerFailsWhenPersonalityIsInvalid() {
		assertThatThrownBy(() -> service.register(1L, registerInfo("MALE", Set.of("BRAVE"))))
				.isInstanceOf(InvalidPersonalityException.class);
	}
}
