package com.daesabu.meongcoach.dog.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegisteredBreedTest {

	private static final int BREED_COLUMN_LENGTH = 30;
	private static final List<String> LEGACY_CODES = List.of("MALTESE", "POODLE", "POMERANIAN", "BICHON_FRISE",
			"CHIHUAHUA", "SHIH_TZU", "JINDO", "YORKSHIRE_TERRIER", "SPITZ", "WELSH_CORGI", "DACHSHUND", "SCHNAUZER",
			"GOLDEN_RETRIEVER", "LABRADOR_RETRIEVER", "SHIBA_INU", "BORDER_COLLIE", "PEKINGESE", "COCKER_SPANIEL",
			"FRENCH_BULLDOG", "PUG", "PAPILLON", "SAMOYED", "SIBERIAN_HUSKY", "GERMAN_SHEPHERD", "BEAGLE",
			"ITALIAN_GREYHOUND", "JACK_RUSSELL_TERRIER", "CAVALIER_KING_CHARLES_SPANIEL", "BOSTON_TERRIER",
			"DOBERMANN", "MIXED");

	@Test
	void 믹스견을_맨_앞에_둔다() {
		assertThat(RegisteredBreed.values()[0]).isEqualTo(RegisteredBreed.MIXED);
	}

	@Test
	void 믹스견_뒤의_견종은_라벨_가나다순으로_선언한다() {
		List<String> labels = Arrays.stream(RegisteredBreed.values())
				.skip(1)
				.map(RegisteredBreed::getLabel)
				.toList();

		assertThat(labels).isSorted();
	}

	@Test
	void 세부_종류를_모르는_부모_견종은_접미사_없는_라벨로_표시하고_원래_라벨로도_검색된다() {
		assertThat(RegisteredBreed.POODLE.getLabel()).isEqualTo("푸들");
		assertThat(RegisteredBreed.POODLE.getAliases()).contains("푸들 (세부 종류 모름)");
	}

	@Test
	void 상수_이름은_breed_컬럼_길이를_넘지_않는다() {
		assertThat(RegisteredBreed.values())
				.allSatisfy(breed -> assertThat(breed.name()).hasSizeLessThanOrEqualTo(BREED_COLUMN_LENGTH));
	}

	@Test
	void 이미_저장된_기존_견종_코드를_모두_보존한다() {
		assertThat(LEGACY_CODES).allSatisfy(code -> assertThat(RegisteredBreed.find(code)).isPresent());
	}

	@Test
	void 라벨로_견종을_찾는다() {
		assertThat(RegisteredBreed.findByLabel("푸들")).contains(RegisteredBreed.POODLE);
	}

	@Test
	void 목록에_없는_이름과_라벨은_찾지_못한다() {
		assertThat(RegisteredBreed.find("삽살개")).isEmpty();
		assertThat(RegisteredBreed.find(null)).isEmpty();
		assertThat(RegisteredBreed.findByLabel("삽살개")).isEmpty();
		assertThat(RegisteredBreed.findByLabel(null)).isEmpty();
	}
}
