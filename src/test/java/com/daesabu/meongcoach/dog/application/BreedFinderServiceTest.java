package com.daesabu.meongcoach.dog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.dog.application.provided.BreedInfo;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("강아지 견종 조회 서비스")
class BreedFinderServiceTest {

	private final BreedFinderService service = new BreedFinderService();

	@Test
	@DisplayName("모든 견종을 코드와 한글 라벨로 반환한다")
	void findAllReturnsAllBreedsWithLabels() {
		List<BreedInfo> breeds = service.findAll();

		assertThat(breeds).containsExactly(
				new BreedInfo("POODLE", "푸들"),
				new BreedInfo("MALTESE", "말티즈"),
				new BreedInfo("POMERANIAN", "포메라니안"),
				new BreedInfo("SHIH_TZU", "시츄"),
				new BreedInfo("JINDO", "진돗개"),
				new BreedInfo("GOLDEN_RETRIEVER", "골든 리트리버"),
				new BreedInfo("MIXED", "믹스견"));
	}
}
