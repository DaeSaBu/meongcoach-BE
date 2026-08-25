package com.daesabu.meongcoach.dog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.domain.exception.InvalidBreedException;
import com.daesabu.meongcoach.dog.domain.exception.InvalidDogSexException;
import com.daesabu.meongcoach.dog.domain.exception.InvalidPersonalityException;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.shared.DogSex;
import com.daesabu.meongcoach.dog.domain.shared.Personality;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DogTest {

	private Dog registerDog() {
		return registerDog(LocalDate.of(2024, 3, 1));
	}

	private Dog registerDog(LocalDate birthDate) {
		return Dog.register(1L, new DogRegisterCommand("초코", "POODLE", "MALE", birthDate,
				new BigDecimal("4.50"), null, null, null));
	}

	private DogRegisterCommand registerCommand(String breed, String sex, Set<String> personalities) {
		return new DogRegisterCommand("초코", breed, sex, LocalDate.of(2024, 3, 1),
				new BigDecimal("4.50"), personalities, null, null);
	}

	@Test
	void 등록하면_미선택_상태의_강아지가_생성된다() {
		Dog dog = registerDog();

		assertThat(dog.getUserId()).isEqualTo(1L);
		assertThat(dog.getName()).isEqualTo("초코");
		assertThat(dog.getBreed()).isEqualTo(Breed.POODLE);
		assertThat(dog.getSex()).isEqualTo(DogSex.MALE);
		assertThat(dog.getBirthDate()).isEqualTo(LocalDate.of(2024, 3, 1));
		assertThat(dog.getWeightKg()).isEqualByComparingTo("4.50");
		assertThat(dog.getStatus()).isEqualTo(DogStatus.UNSELECTED);
		assertThat(dog.getProfileImageUrl()).isEmpty();
		assertThat(dog.getExpectation()).isEmpty();
	}

	@Test
	void 등록_시_프로필_이미지와_기대사항을_저장한다() {
		Dog dog = Dog.register(1L, new DogRegisterCommand("초코", "POODLE", "MALE", LocalDate.of(2024, 3, 1),
				new BigDecimal("4.50"), null, "https://cdn.meongcoach.com/dog.png", "다른 강아지와 편안하게 인사했으면 좋겠어요."));

		assertThat(dog.getProfileImageUrl()).isEqualTo("https://cdn.meongcoach.com/dog.png");
		assertThat(dog.getExpectation()).isEqualTo("다른 강아지와 편안하게 인사했으면 좋겠어요.");
	}

	@Test
	void 등록_시_성격_코드를_enum으로_변환해_저장한다() {
		Dog dog = Dog.register(1L, registerCommand("POODLE", "MALE", Set.of("TIMID", "LIVELY")));

		assertThat(dog.getPersonalities()).containsExactlyInAnyOrder(Personality.TIMID, Personality.LIVELY);
	}

	@Test
	void 성격이_null이면_빈_성격으로_등록된다() {
		Dog dog = registerDog();

		assertThat(dog.getPersonalities()).isEmpty();
	}

	@Test
	void 잘못된_견종_코드로_등록하면_실패한다() {
		assertThatThrownBy(() -> Dog.register(1L, registerCommand("UNKNOWN", "MALE", Set.of())))
				.isInstanceOf(InvalidBreedException.class);
	}

	@Test
	void 잘못된_성별_코드로_등록하면_실패한다() {
		assertThatThrownBy(() -> Dog.register(1L, registerCommand("POODLE", "UNKNOWN", Set.of())))
				.isInstanceOf(InvalidDogSexException.class);
	}

	@Test
	void 잘못된_성격_코드로_등록하면_실패한다() {
		assertThatThrownBy(() -> Dog.register(1L, registerCommand("POODLE", "MALE", Set.of("BRAVE"))))
				.isInstanceOf(InvalidPersonalityException.class);
	}

	@Test
	void 프로필을_수정하면_이름_견종_성별_생년월일_몸무게_성격_이미지_기대사항이_교체된다() {
		Dog dog = Dog.register(1L, registerCommand("POODLE", "MALE", Set.of("FRIENDLY")));

		dog.updateProfile(new DogProfileUpdateCommand("보리", "MALTESE", "FEMALE",
				LocalDate.of(2023, 1, 15), new BigDecimal("3.20"), Set.of("TIMID", "LIVELY"),
				"https://cdn.meongcoach.com/bori.png", "산책 예절을 배우고 싶어요."));

		assertThat(dog.getName()).isEqualTo("보리");
		assertThat(dog.getBreed()).isEqualTo(Breed.MALTESE);
		assertThat(dog.getSex()).isEqualTo(DogSex.FEMALE);
		assertThat(dog.getBirthDate()).isEqualTo(LocalDate.of(2023, 1, 15));
		assertThat(dog.getWeightKg()).isEqualByComparingTo("3.20");
		assertThat(dog.getPersonalities()).containsExactlyInAnyOrder(Personality.TIMID, Personality.LIVELY);
		assertThat(dog.getProfileImageUrl()).isEqualTo("https://cdn.meongcoach.com/bori.png");
		assertThat(dog.getExpectation()).isEqualTo("산책 예절을 배우고 싶어요.");
	}

	@Test
	void 프로필_수정은_소유자와_선택_상태를_바꾸지_않는다() {
		Dog dog = registerDog();
		dog.select();

		dog.updateProfile(new DogProfileUpdateCommand("보리", "MALTESE", "FEMALE", null,
				new BigDecimal("3.20"), Set.of(), null, null));

		assertThat(dog.getUserId()).isEqualTo(1L);
		assertThat(dog.getStatus()).isEqualTo(DogStatus.SELECTED);
	}

	@Test
	void 프로필_수정_시_성격이_null이면_빈_성격으로_교체된다() {
		Dog dog = Dog.register(1L, registerCommand("POODLE", "MALE", Set.of("FRIENDLY")));

		dog.updateProfile(new DogProfileUpdateCommand("보리", "MALTESE", "FEMALE", null,
				new BigDecimal("3.20"), null, null, null));

		assertThat(dog.getPersonalities()).isEmpty();
	}

	@Test
	void 잘못된_견종_코드로_수정하면_실패한다() {
		Dog dog = registerDog();

		assertThatThrownBy(() -> dog.updateProfile(new DogProfileUpdateCommand("보리", "UNKNOWN", "FEMALE", null,
				new BigDecimal("3.20"), Set.of(), null, null)))
				.isInstanceOf(InvalidBreedException.class);
	}

	@Test
	void 프로필_수정_시_생년월일이_null이면_나이_미상으로_바뀐다() {
		Dog dog = registerDog();

		dog.updateProfile(new DogProfileUpdateCommand("보리", "MALTESE", "FEMALE", null,
				new BigDecimal("3.20"), Set.of(), null, null));

		assertThat(dog.getBirthDate()).isNull();
		assertThat(dog.getAge()).isNull();
	}

	@Test
	void 프로필_수정_시_이미지와_기대사항이_null이면_빈_문자열로_교체된다() {
		Dog dog = Dog.register(1L, new DogRegisterCommand("초코", "POODLE", "MALE", LocalDate.of(2024, 3, 1),
				new BigDecimal("4.50"), null, "https://cdn.meongcoach.com/dog.png", "기존 기대 사항"));

		dog.updateProfile(new DogProfileUpdateCommand("보리", "MALTESE", "FEMALE", null,
				new BigDecimal("3.20"), Set.of(), null, null));

		assertThat(dog.getProfileImageUrl()).isEmpty();
		assertThat(dog.getExpectation()).isEmpty();
	}

	@Test
	void 생년월일로_나이를_계산한다() {
		Dog dog = registerDog(LocalDate.now().minusYears(3));

		assertThat(dog.getAge()).isEqualTo(3);
	}

	@Test
	void 생년월일이_없으면_나이는_null을_반환한다() {
		Dog dog = registerDog(null);

		assertThat(dog.getAge()).isNull();
	}

	@Test
	void 선택_해제하면_상태가_UNSELECTED로_변경된다() {
		Dog dog = registerDog();

		dog.unselect();

		assertThat(dog.getStatus()).isEqualTo(DogStatus.UNSELECTED);
	}

	@Test
	void 선택하면_상태가_SELECTED로_변경된다() {
		Dog dog = registerDog();
		dog.unselect();

		dog.select();

		assertThat(dog.getStatus()).isEqualTo(DogStatus.SELECTED);
	}

	@Test
	void 등록_직후에는_삭제_시각이_없다() {
		Dog dog = registerDog();

		assertThat(dog.getDeletedAt()).isNull();
	}

	@Test
	void 삭제하면_삭제_시각이_기록된다() {
		Dog dog = registerDog();

		dog.delete();

		assertThat(dog.getDeletedAt()).isNotNull();
	}
}
