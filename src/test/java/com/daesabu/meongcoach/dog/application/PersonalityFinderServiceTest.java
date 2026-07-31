package com.daesabu.meongcoach.dog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.dog.application.provided.PersonalityInfo;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("강아지 성격 조회 서비스")
class PersonalityFinderServiceTest {

	private final PersonalityFinderService service = new PersonalityFinderService();

	@Test
	@DisplayName("모든 성격을 코드와 한글 라벨로 반환한다")
	void findAllReturnsAllPersonalitiesWithLabels() {
		List<PersonalityInfo> personalities = service.findAll();

		assertThat(personalities).containsExactly(
				new PersonalityInfo("TIMID", "소심함"),
				new PersonalityInfo("LIVELY", "활발함"),
				new PersonalityInfo("FRIENDLY", "친화적"));
	}
}
