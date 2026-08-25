package com.daesabu.meongcoach.dog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.dog.domain.shared.Breed;
import java.util.List;
import org.junit.jupiter.api.Test;

class BreedFinderServiceTest {

	private final BreedFinderService service = new BreedFinderService();

	@Test
	void 모든_견종을_선언_순서대로_반환하고_믹스견이_마지막이다() {
		List<Breed> breeds = service.findAll();

		assertThat(breeds).containsExactly(Breed.values());
		assertThat(breeds).last().isEqualTo(Breed.MIXED);
	}
}
