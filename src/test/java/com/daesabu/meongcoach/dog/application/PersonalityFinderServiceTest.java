package com.daesabu.meongcoach.dog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.dog.application.provided.PersonalityInfo;
import java.util.List;
import org.junit.jupiter.api.Test;

class PersonalityFinderServiceTest {

	private final PersonalityFinderService service = new PersonalityFinderService();

	@Test
	void 모든_성격을_코드와_한글_라벨로_반환한다() {
		List<PersonalityInfo> personalities = service.findAll();

		assertThat(personalities).containsExactly(
				new PersonalityInfo("TIMID", "소심함"),
				new PersonalityInfo("LIVELY", "활발함"),
				new PersonalityInfo("FRIENDLY", "친화적"),
				new PersonalityInfo("CALM", "차분함"),
				new PersonalityInfo("FEARFUL", "겁 많음"),
				new PersonalityInfo("AFFECTIONATE", "애교 많음"),
				new PersonalityInfo("INDEPENDENT", "독립적"),
				new PersonalityInfo("PLAYFUL", "장난기 많음"),
				new PersonalityInfo("EXCITABLE", "쉽게 흥분함"),
				new PersonalityInfo("STUBBORN", "고집 셈"));
	}
}
