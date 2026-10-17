package com.daesabu.meongcoach.dog.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.domain.exception.BreedLengthExceededException;
import com.daesabu.meongcoach.dog.domain.exception.BreedRequiredException;
import com.daesabu.meongcoach.dog.domain.exception.DogErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BreedTest {

	@Test
	void 등록_견종은_상수_이름을_값으로_가진다() {
		Breed breed = Breed.of(RegisteredBreed.POODLE);

		assertThat(breed.value()).isEqualTo("POODLE");
	}

	@Test
	void 등록_견종의_표시명은_한글_라벨이다() {
		assertThat(Breed.of(RegisteredBreed.POODLE).label()).isEqualTo("푸들");
	}

	@Test
	void 등록_견종_코드_문자열로_만들면_등록_견종이다() {
		assertThat(Breed.from("POODLE")).isEqualTo(Breed.of(RegisteredBreed.POODLE));
	}

	@Test
	void 목록에_없는_이름으로_만들면_직접_입력한_견종이다() {
		Breed breed = Breed.from("삽살개");

		assertThat(breed.value()).isEqualTo("삽살개");
	}

	@Test
	void 직접_입력한_견종의_표시명은_입력한_이름이다() {
		assertThat(Breed.from("삽살개").label()).isEqualTo("삽살개");
	}

	@Test
	void 입력한_이름의_앞뒤_공백을_제거한다() {
		assertThat(Breed.from("  삽살개 ").value()).isEqualTo("삽살개");
	}

	@Test
	void 등록_견종의_한글_라벨을_입력하면_해당_등록_견종이_된다() {
		assertThat(Breed.from("말티즈")).isEqualTo(Breed.of(RegisteredBreed.MALTESE));
		assertThat(Breed.from(" 말티즈 ")).isEqualTo(Breed.of(RegisteredBreed.MALTESE));
	}

	@Test
	void 같은_값끼리는_동등하다() {
		assertThat(Breed.from("삽살개")).isEqualTo(Breed.from("삽살개"));
	}

	@Test
	void 값이_null이면_생성에_실패한다() {
		assertThatThrownBy(() -> Breed.from(null))
				.isInstanceOf(BreedRequiredException.class)
				.extracting("errorCode")
				.isEqualTo(DogErrorCode.DOG_BREED_REQUIRED);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " ", "   "})
	void 값이_비어_있으면_생성에_실패한다(String value) {
		assertThatThrownBy(() -> Breed.from(value))
				.isInstanceOf(BreedRequiredException.class)
				.extracting("errorCode")
				.isEqualTo(DogErrorCode.DOG_BREED_REQUIRED);
	}

	@Test
	void 값이_최대_길이면_생성에_성공한다() {
		String name = "가".repeat(Breed.MAX_LENGTH);

		assertThat(Breed.from(name).value()).isEqualTo(name);
	}

	@Test
	void 앞뒤_공백을_제거한_길이가_최대_길이면_생성에_성공한다() {
		String name = "가".repeat(Breed.MAX_LENGTH);

		assertThat(Breed.from(" " + name + " ").value()).isEqualTo(name);
	}

	@Test
	void 값이_최대_길이를_넘으면_생성에_실패한다() {
		String name = "가".repeat(Breed.MAX_LENGTH + 1);

		assertThatThrownBy(() -> Breed.from(name))
				.isInstanceOf(BreedLengthExceededException.class)
				.extracting("errorCode")
				.isEqualTo(DogErrorCode.DOG_BREED_LENGTH_EXCEEDED);
	}

	@Test
	void 생성_실패_메시지에_입력한_값을_담지_않는다() {
		String name = "비밀".repeat(Breed.MAX_LENGTH);

		assertThatThrownBy(() -> Breed.from(name))
				.isInstanceOf(BreedLengthExceededException.class)
				.hasMessageNotContaining("비밀");
	}
}
