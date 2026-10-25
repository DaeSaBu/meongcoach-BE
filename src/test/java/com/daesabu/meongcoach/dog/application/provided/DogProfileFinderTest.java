package com.daesabu.meongcoach.dog.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.application.provided.dto.DogSummaryResult;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.RegisteredBreed;
import com.daesabu.meongcoach.support.ApplicationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class DogProfileFinderTest {

	@Autowired
	private DogProfileFinder finder;

	@Autowired
	private DogRepository dogRepository;

	@Test
	void 본인의_선택된_강아지_ID와_이름을_반환한다() {
		dogRepository.save(newDog(42L, "보리"));
		Dog selected = newDog(42L, "초코");
		selected.select();
		dogRepository.save(selected);

		DogSummaryResult result = finder.findSelectedDogSummary(42L);

		assertThat(result.id()).isEqualTo(selected.getId());
		assertThat(result.name()).isEqualTo("초코");
	}

	@Test
	void 미선택_강아지만_있으면_요약_조회도_거절한다() {
		dogRepository.save(newDog(42L, "초코"));

		assertThatThrownBy(() -> finder.findSelectedDogSummary(42L))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	void 다른_사용자의_선택된_강아지는_요약에_반환하지_않는다() {
		Dog selected = newDog(99L, "초코");
		selected.select();
		dogRepository.save(selected);

		assertThatThrownBy(() -> finder.findSelectedDogSummary(42L))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	void 강아지가_없으면_요약_조회도_거절한다() {
		assertThatThrownBy(() -> finder.findSelectedDogSummary(42L))
				.isInstanceOf(DogNotFoundException.class);
	}

	private Dog newDog(Long userId, String name) {
		DogRegisterCommand command = new DogRegisterCommand(userId, name, Breed.of(RegisteredBreed.POODLE),
				DogSex.MALE, LocalDate.of(2024, 3, 1), new BigDecimal("4.50"), null, null);
		return Dog.register(command);
	}
}
