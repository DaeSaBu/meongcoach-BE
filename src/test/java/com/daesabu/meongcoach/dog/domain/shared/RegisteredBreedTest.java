package com.daesabu.meongcoach.dog.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

class RegisteredBreedTest {

	private static final int BREED_COLUMN_LENGTH = 30;
	private static final List<String> LEGACY_CODES = List.of("MALTESE", "POODLE", "POMERANIAN", "BICHON_FRISE",
			"CHIHUAHUA", "SHIH_TZU", "JINDO", "YORKSHIRE_TERRIER", "SPITZ", "WELSH_CORGI", "DACHSHUND", "SCHNAUZER",
			"GOLDEN_RETRIEVER", "LABRADOR_RETRIEVER", "SHIBA_INU", "BORDER_COLLIE", "PEKINGESE", "COCKER_SPANIEL",
			"FRENCH_BULLDOG", "PUG", "PAPILLON", "SAMOYED", "SIBERIAN_HUSKY", "GERMAN_SHEPHERD", "BEAGLE",
			"ITALIAN_GREYHOUND", "JACK_RUSSELL_TERRIER", "CAVALIER_KING_CHARLES_SPANIEL", "BOSTON_TERRIER",
			"DOBERMANN", "MIXED");

	@Test
	void 선언_순서와_라벨과_별칭이_조사한_견종_목록과_같다() {
		List<BreedChoice> expected = readChoices();

		List<BreedChoice> actual = Arrays.stream(RegisteredBreed.values())
				.map(breed -> new BreedChoice(breed.name(), breed.getLabel(), breed.getAliases()))
				.toList();

		assertThat(actual).containsExactlyElementsOf(expected);
	}

	@Test
	void 믹스견을_맨_앞에_둔다() {
		assertThat(RegisteredBreed.values()[0]).isEqualTo(RegisteredBreed.MIXED);
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

	private List<BreedChoice> readChoices() {
		InputStream fixture = getClass().getResourceAsStream("/dog/breed-choices.json");
		return new ObjectMapper().readValue(fixture, new TypeReference<>() {
		});
	}

	private record BreedChoice(String code, String label, List<String> aliases) {
	}
}
