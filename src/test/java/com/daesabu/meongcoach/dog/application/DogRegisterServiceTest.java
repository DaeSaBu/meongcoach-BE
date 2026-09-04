package com.daesabu.meongcoach.dog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.application.provided.DogRegisterInfo;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Breed;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import com.daesabu.meongcoach.dog.domain.Dogs;
import com.daesabu.meongcoach.dog.domain.Personality;
import com.daesabu.meongcoach.dog.domain.exception.DogLimitExceededException;
import com.daesabu.meongcoach.dog.domain.exception.InvalidBreedException;
import com.daesabu.meongcoach.dog.domain.exception.InvalidDogSexException;
import com.daesabu.meongcoach.dog.domain.exception.InvalidPersonalityException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class DogRegisterServiceTest {

	@Autowired
	private DogRepository dogRepository;

	@Autowired
	private TestEntityManager entityManager;

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
	void 강아지를_등록하면_성격과_함께_저장된다() {
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
	void 두_번째로_등록한_강아지는_미선택_상태가_된다() {
		Long firstDogId = service.register(1L, registerInfo("MALE", Set.of()));

		Long secondDogId = service.register(1L, registerInfo("FEMALE", Set.of()));

		Dog firstDog = dogRepository.findById(firstDogId).orElseThrow();
		Dog secondDog = dogRepository.findById(secondDogId).orElseThrow();
		assertThat(firstDog.getStatus()).isEqualTo(DogStatus.SELECTED);
		assertThat(secondDog.getStatus()).isEqualTo(DogStatus.UNSELECTED);
	}

	@Test
	void 다른_사용자의_선택된_강아지는_선택_여부에_영향을_주지_않는다() {
		service.register(1L, registerInfo("MALE", Set.of()));

		Long otherUserDogId = service.register(2L, registerInfo("MALE", Set.of()));

		Dog otherUserDog = dogRepository.findById(otherUserDogId).orElseThrow();
		assertThat(otherUserDog.getStatus()).isEqualTo(DogStatus.SELECTED);
	}

	@Test
	void 성격이_없으면_빈_성격으로_등록된다() {
		Long dogId = service.register(1L, registerInfo("FEMALE", null));

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getPersonalities()).isEmpty();
	}

	@Test
	void 생년월일이_없어도_등록할_수_있다() {
		DogRegisterInfo info = new DogRegisterInfo("초코", "POODLE", "MALE", null,
				new BigDecimal("4.50"), Set.of(), null, null);

		Long dogId = service.register(1L, info);

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getBirthDate()).isNull();
	}

	@Test
	void 프로필_이미지_URL을_함께_저장한다() {
		String imageUrl = "https://images.test.meongcoach.com/images/dog-profile/1/a.jpg";
		DogRegisterInfo info = new DogRegisterInfo("초코", "POODLE", "MALE", null,
				new BigDecimal("4.50"), Set.of(), imageUrl, null);

		Long dogId = service.register(1L, info);

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getProfileImageUrl()).isEqualTo(imageUrl);
	}

	@Test
	void 프로필_이미지가_없으면_빈_문자열로_저장한다() {
		Long dogId = service.register(1L, registerInfo("MALE", Set.of()));

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getProfileImageUrl()).isEmpty();
	}

	@Test
	void 기대_사항을_함께_저장한다() {
		DogRegisterInfo info = new DogRegisterInfo("초코", "POODLE", "MALE", null,
				new BigDecimal("4.50"), Set.of(), null, "보호자와 즐겁게 교육받고 싶어요.");

		Long dogId = service.register(1L, info);

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getExpectation()).isEqualTo("보호자와 즐겁게 교육받고 싶어요.");
	}

	@Test
	void 기대_사항이_없으면_빈_문자열로_저장한다() {
		Long dogId = service.register(1L, registerInfo("MALE", Set.of()));

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getExpectation()).isEmpty();
	}

	@Test
	void 잘못된_견종_값이면_등록에_실패한다() {
		DogRegisterInfo info = new DogRegisterInfo("초코", "UNKNOWN", "MALE", null,
				new BigDecimal("4.50"), Set.of(), null, null);

		assertThatThrownBy(() -> service.register(1L, info))
				.isInstanceOf(InvalidBreedException.class);
	}

	@Test
	void 견종_값이_없으면_등록에_실패한다() {
		DogRegisterInfo info = new DogRegisterInfo("초코", null, "MALE", null,
				new BigDecimal("4.50"), Set.of(), null, null);

		assertThatThrownBy(() -> service.register(1L, info))
				.isInstanceOf(InvalidBreedException.class);
	}

	@Test
	void 잘못된_성별_값이면_등록에_실패한다() {
		assertThatThrownBy(() -> service.register(1L, registerInfo("UNKNOWN", Set.of())))
				.isInstanceOf(InvalidDogSexException.class);
	}

	@Test
	void 성별_값이_없으면_등록에_실패한다() {
		assertThatThrownBy(() -> service.register(1L, registerInfo(null, Set.of())))
				.isInstanceOf(InvalidDogSexException.class);
	}

	@Test
	void 잘못된_성격_값이면_등록에_실패한다() {
		assertThatThrownBy(() -> service.register(1L, registerInfo("MALE", Set.of("BRAVE"))))
				.isInstanceOf(InvalidPersonalityException.class);
	}

	@Test
	void 강아지가_5마리면_더_등록할_수_없다() {
		registerDogs(1L, 5);

		assertThatThrownBy(() -> service.register(1L, registerInfo("MALE", Set.of())))
				.isInstanceOf(DogLimitExceededException.class);

		assertThat(dogRepository.findAllByUserIdOrderByIdAsc(1L)).hasSize(5);
	}

	@Test
	void 삭제된_강아지는_등록_개수에_포함되지_않는다() {
		Long deletedDogId = registerDogs(1L, 5).getFirst();
		Dogs dogs = new Dogs(dogRepository.findAllByUserIdOrderByIdAsc(1L));
		dogs.delete(deletedDogId);
		entityManager.flush();
		entityManager.clear();

		Long dogId = service.register(1L, registerInfo("MALE", Set.of()));

		assertThat(dogRepository.findById(dogId)).isPresent();
		assertThat(dogRepository.findAllByUserIdOrderByIdAsc(1L)).hasSize(5);
	}

	@Test
	void 다른_사용자의_강아지는_등록_개수에_포함되지_않는다() {
		registerDogs(1L, 5);

		Long otherUserDogId = service.register(2L, registerInfo("MALE", Set.of()));

		assertThat(dogRepository.findById(otherUserDogId)).isPresent();
	}

	private List<Long> registerDogs(Long userId, int count) {
		return IntStream.range(0, count)
				.mapToObj(i -> service.register(userId, registerInfo("MALE", Set.of())))
				.toList();
	}
}
