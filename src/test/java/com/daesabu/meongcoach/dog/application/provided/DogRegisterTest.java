package com.daesabu.meongcoach.dog.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import com.daesabu.meongcoach.dog.domain.Dogs;
import com.daesabu.meongcoach.dog.domain.exception.BreedRequiredException;
import com.daesabu.meongcoach.dog.domain.exception.DogLimitExceededException;
import com.daesabu.meongcoach.dog.domain.exception.InvalidDogSexException;
import com.daesabu.meongcoach.dog.domain.exception.InvalidPersonalityException;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.Personality;
import com.daesabu.meongcoach.dog.domain.shared.RegisteredBreed;
import com.daesabu.meongcoach.media.domain.exception.InvalidMediaUrlException;
import com.daesabu.meongcoach.support.ApplicationTest;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class DogRegisterTest {

	@Autowired
	private DogRepository dogRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private DogRegister dogRegister;

	private DogRegisterInfo registerInfo(String sex, Set<String> personalities) {
		return new DogRegisterInfo("초코", "POODLE", sex, LocalDate.of(2024, 3, 1),
				new BigDecimal("4.50"), personalities, null, null);
	}

	@Test
	void 강아지를_등록하면_성격과_함께_저장된다() {
		Long dogId = dogRegister.register(1L, registerInfo("MALE", Set.of("TIMID", "LIVELY")));

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getUserId()).isEqualTo(1L);
		assertThat(dog.getName()).isEqualTo("초코");
		assertThat(dog.getBreed()).isEqualTo(Breed.of(RegisteredBreed.POODLE));
		assertThat(dog.getSex()).isEqualTo(DogSex.MALE);
		assertThat(dog.getStatus()).isEqualTo(DogStatus.SELECTED);
		assertThat(dog.getPersonalities()).containsExactlyInAnyOrder(Personality.TIMID, Personality.LIVELY);
	}

	@Test
	void 두_번째로_등록한_강아지는_미선택_상태가_된다() {
		Long firstDogId = dogRegister.register(1L, registerInfo("MALE", Set.of()));

		Long secondDogId = dogRegister.register(1L, registerInfo("FEMALE", Set.of()));

		Dog firstDog = dogRepository.findById(firstDogId).orElseThrow();
		Dog secondDog = dogRepository.findById(secondDogId).orElseThrow();
		assertThat(firstDog.getStatus()).isEqualTo(DogStatus.SELECTED);
		assertThat(secondDog.getStatus()).isEqualTo(DogStatus.UNSELECTED);
	}

	@Test
	void 다른_사용자의_선택된_강아지는_선택_여부에_영향을_주지_않는다() {
		dogRegister.register(1L, registerInfo("MALE", Set.of()));

		Long otherUserDogId = dogRegister.register(2L, registerInfo("MALE", Set.of()));

		Dog otherUserDog = dogRepository.findById(otherUserDogId).orElseThrow();
		assertThat(otherUserDog.getStatus()).isEqualTo(DogStatus.SELECTED);
	}

	@Test
	void 성격이_없으면_빈_성격으로_등록된다() {
		Long dogId = dogRegister.register(1L, registerInfo("FEMALE", null));

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getPersonalities()).isEmpty();
	}

	@Test
	void 생년월일이_없어도_등록할_수_있다() {
		DogRegisterInfo info = new DogRegisterInfo("초코", "POODLE", "MALE", null,
				new BigDecimal("4.50"), Set.of(), null, null);

		Long dogId = dogRegister.register(1L, info);

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getBirthDate()).isNull();
	}

	@Test
	void 프로필_이미지_URL을_함께_저장한다() {
		String imageUrl = "https://images.test.meongcoach.com/images/dog-profile/1/a.jpg";
		DogRegisterInfo info = new DogRegisterInfo("초코", "POODLE", "MALE", null,
				new BigDecimal("4.50"), Set.of(), imageUrl, null);

		Long dogId = dogRegister.register(1L, info);

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getProfileImageUrl()).isEqualTo(imageUrl);
	}

	@Test
	void 우리_스토리지의_URL이_아닌_프로필_이미지면_등록에_실패한다() {
		DogRegisterInfo info = new DogRegisterInfo("초코", "POODLE", "MALE", null,
				new BigDecimal("4.50"), Set.of(), "https://evil.example.com/a.jpg", null);

		assertThatThrownBy(() -> dogRegister.register(1L, info))
				.isInstanceOf(InvalidMediaUrlException.class);
		assertThat(dogRepository.findAllByUserIdOrderByIdAsc(1L)).isEmpty();
	}

	@Test
	void 다른_사용자가_올린_프로필_이미지면_등록에_실패한다() {
		DogRegisterInfo info = new DogRegisterInfo("초코", "POODLE", "MALE", null,
				new BigDecimal("4.50"), Set.of(), "https://images.test.meongcoach.com/images/dog-profile/2/a.jpg", null);

		assertThatThrownBy(() -> dogRegister.register(1L, info))
				.isInstanceOf(InvalidMediaUrlException.class);
	}

	@Test
	void 강아지_프로필_경로가_아닌_이미지면_등록에_실패한다() {
		DogRegisterInfo info = new DogRegisterInfo("초코", "POODLE", "MALE", null,
				new BigDecimal("4.50"), Set.of(), "https://images.test.meongcoach.com/images/user-profile/1/a.jpg", null);

		assertThatThrownBy(() -> dogRegister.register(1L, info))
				.isInstanceOf(InvalidMediaUrlException.class);
	}

	@Test
	void 프로필_이미지가_없으면_빈_문자열로_저장한다() {
		Long dogId = dogRegister.register(1L, registerInfo("MALE", Set.of()));

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getProfileImageUrl()).isEmpty();
	}

	@Test
	void 기대_사항을_함께_저장한다() {
		DogRegisterInfo info = new DogRegisterInfo("초코", "POODLE", "MALE", null,
				new BigDecimal("4.50"), Set.of(), null, "보호자와 즐겁게 교육받고 싶어요.");

		Long dogId = dogRegister.register(1L, info);

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getExpectation()).isEqualTo("보호자와 즐겁게 교육받고 싶어요.");
	}

	@Test
	void 기대_사항이_없으면_빈_문자열로_저장한다() {
		Long dogId = dogRegister.register(1L, registerInfo("MALE", Set.of()));

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getExpectation()).isEmpty();
	}

	@Test
	void 목록에_없는_견종_이름은_직접_입력한_견종으로_등록된다() {
		DogRegisterInfo info = new DogRegisterInfo("초코", "삽살개", "MALE", null,
				new BigDecimal("4.50"), Set.of(), null, null);

		Long dogId = dogRegister.register(1L, info);

		entityManager.flush();
		entityManager.clear();
		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getBreed()).isEqualTo(Breed.from("삽살개"));
		assertThat(dog.getBreed().label()).isEqualTo("삽살개");
	}

	@Test
	void 등록_견종의_한글_라벨을_보내면_해당_견종으로_등록된다() {
		DogRegisterInfo info = new DogRegisterInfo("초코", "말티즈", "MALE", null,
				new BigDecimal("4.50"), Set.of(), null, null);

		Long dogId = dogRegister.register(1L, info);

		Dog dog = dogRepository.findById(dogId).orElseThrow();
		assertThat(dog.getBreed()).isEqualTo(Breed.of(RegisteredBreed.MALTESE));
	}

	@Test
	void 견종_값이_없으면_등록에_실패한다() {
		DogRegisterInfo info = new DogRegisterInfo("초코", null, "MALE", null,
				new BigDecimal("4.50"), Set.of(), null, null);

		assertThatThrownBy(() -> dogRegister.register(1L, info))
				.isInstanceOf(BreedRequiredException.class);
	}

	@Test
	void 잘못된_성별_값이면_등록에_실패한다() {
		assertThatThrownBy(() -> dogRegister.register(1L, registerInfo("UNKNOWN", Set.of())))
				.isInstanceOf(InvalidDogSexException.class);
	}

	@Test
	void 성별_값이_없으면_등록에_실패한다() {
		assertThatThrownBy(() -> dogRegister.register(1L, registerInfo(null, Set.of())))
				.isInstanceOf(InvalidDogSexException.class);
	}

	@Test
	void 잘못된_성격_값이면_등록에_실패한다() {
		assertThatThrownBy(() -> dogRegister.register(1L, registerInfo("MALE", Set.of("BRAVE"))))
				.isInstanceOf(InvalidPersonalityException.class);
	}

	@Test
	void 강아지가_5마리면_더_등록할_수_없다() {
		registerDogs(1L, 5);

		assertThatThrownBy(() -> dogRegister.register(1L, registerInfo("MALE", Set.of())))
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

		Long dogId = dogRegister.register(1L, registerInfo("MALE", Set.of()));

		assertThat(dogRepository.findById(dogId)).isPresent();
		assertThat(dogRepository.findAllByUserIdOrderByIdAsc(1L)).hasSize(5);
	}

	@Test
	void 다른_사용자의_강아지는_등록_개수에_포함되지_않는다() {
		registerDogs(1L, 5);

		Long otherUserDogId = dogRegister.register(2L, registerInfo("MALE", Set.of()));

		assertThat(dogRepository.findById(otherUserDogId)).isPresent();
	}

	private List<Long> registerDogs(Long userId, int count) {
		return IntStream.range(0, count)
				.mapToObj(i -> dogRegister.register(userId, registerInfo("MALE", Set.of())))
				.toList();
	}
}
