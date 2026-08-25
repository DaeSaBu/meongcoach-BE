package com.daesabu.meongcoach.dog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.dog.domain.shared.Personality;
import java.util.List;
import org.junit.jupiter.api.Test;

class PersonalityFinderServiceTest {

	private final PersonalityFinderService service = new PersonalityFinderService();

	@Test
	void 모든_성격을_선언_순서대로_반환한다() {
		List<Personality> personalities = service.findAll();

		assertThat(personalities).containsExactly(Personality.values());
	}
}
