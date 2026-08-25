package com.daesabu.meongcoach.dog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.application.provided.DogProfileUpdater;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogProfileUpdateCommand;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.DogSex;
import com.daesabu.meongcoach.dog.domain.shared.Personality;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(DogProfileUpdateService.class)
class DogProfileUpdateServiceTest {

	private static final Long USER_ID = 42L;
	private static final Long OTHER_USER_ID = 99L;
	private static final String IMAGE_URL = "https://images.test.meongcoach.com/images/dog-profile/42/a.jpg";
	private static final String NEW_IMAGE_URL = "https://images.test.meongcoach.com/images/dog-profile/42/b.jpg";
	private static final String EXPECTATION = "보호자와 즐겁게 교육받고 싶어요.";
	private static final String NEW_EXPECTATION = "산책 예절을 배우고 싶어요.";

	@Autowired
	private DogProfileUpdater dogProfileUpdater;

	@Autowired
	private DogRepository dogRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 소유_강아지의_프로필_전체를_교체해_저장한다() {
		Dog saved = persistSelectedDog(USER_ID);
		entityManager.clear();

		dogProfileUpdater.update(USER_ID, saved.getId(), updateCommand(Set.of("TIMID", "LIVELY")));
		entityManager.flush();
		entityManager.clear();

		Dog updated = dogRepository.findByIdAndUserId(saved.getId(), USER_ID).orElseThrow();
		assertThat(updated.getName()).isEqualTo("보리");
		assertThat(updated.getBreed()).isEqualTo(Breed.MALTESE);
		assertThat(updated.getSex()).isEqualTo(DogSex.FEMALE);
		assertThat(updated.getBirthDate()).isEqualTo(LocalDate.of(2023, 1, 15));
		assertThat(updated.getWeightKg()).isEqualByComparingTo(new BigDecimal("3.20"));
		assertThat(updated.getProfileImageUrl()).isEqualTo(NEW_IMAGE_URL);
		assertThat(updated.getExpectation()).isEqualTo(NEW_EXPECTATION);
		assertThat(updated.getPersonalities()).containsExactlyInAnyOrder(Personality.TIMID, Personality.LIVELY);
	}

	@Test
	void 수정된_강아지를_반환한다() {
		Dog saved = persistSelectedDog(USER_ID);

		Dog updated = dogProfileUpdater.update(USER_ID, saved.getId(), updateCommand(Set.of("TIMID")));

		assertThat(updated.getId()).isEqualTo(saved.getId());
		assertThat(updated.getName()).isEqualTo("보리");
		assertThat(updated.getPersonalities()).containsExactly(Personality.TIMID);
	}

	@Test
	void 프로필_수정은_선택_상태와_소유자를_유지한다() {
		Dog saved = persistSelectedDog(USER_ID);

		Dog updated = dogProfileUpdater.update(USER_ID, saved.getId(), updateCommand(Set.of()));

		assertThat(updated.getStatus()).isEqualTo(DogStatus.SELECTED);
		assertThat(updated.getUserId()).isEqualTo(USER_ID);
	}

	@Test
	void 성격을_비우면_빈_성격으로_저장된다() {
		Dog saved = persistSelectedDog(USER_ID);

		dogProfileUpdater.update(USER_ID, saved.getId(), updateCommand(Set.of()));
		entityManager.flush();
		entityManager.clear();

		Dog updated = dogRepository.findByIdAndUserId(saved.getId(), USER_ID).orElseThrow();
		assertThat(updated.getPersonalities()).isEmpty();
	}

	@Test
	void 생년월일과_이미지_기대사항이_없어도_수정할_수_있다() {
		Dog saved = persistSelectedDog(USER_ID);
		DogProfileUpdateCommand command = new DogProfileUpdateCommand("보리", "MALTESE", "FEMALE", null,
				new BigDecimal("3.20"), Set.of(), null, null);

		Dog updated = dogProfileUpdater.update(USER_ID, saved.getId(), command);

		assertThat(updated.getBirthDate()).isNull();
		assertThat(updated.getProfileImageUrl()).isEmpty();
		assertThat(updated.getExpectation()).isEmpty();
	}

	@Test
	void 없는_강아지_ID로_수정하면_예외를_던진다() {
		assertThatThrownBy(() -> dogProfileUpdater.update(USER_ID, 999L, updateCommand(Set.of())))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	void 다른_사용자의_강아지는_수정할_수_없다() {
		Dog othersDog = persistSelectedDog(OTHER_USER_ID);

		assertThatThrownBy(() -> dogProfileUpdater.update(USER_ID, othersDog.getId(),
				updateCommand(Set.of())))
				.isInstanceOf(DogNotFoundException.class);

		entityManager.clear();
		Dog unchanged = dogRepository.findById(othersDog.getId()).orElseThrow();
		assertThat(unchanged.getName()).isEqualTo("초코");
	}

	private DogProfileUpdateCommand updateCommand(Set<String> personalities) {
		return new DogProfileUpdateCommand("보리", "MALTESE", "FEMALE", LocalDate.of(2023, 1, 15),
				new BigDecimal("3.20"), personalities, NEW_IMAGE_URL, NEW_EXPECTATION);
	}

	private Dog persistSelectedDog(Long userId) {
		Dog dog = Dog.register(new DogRegisterCommand(userId, "초코", Breed.POODLE, DogSex.MALE,
				LocalDate.of(2024, 3, 1), new BigDecimal("4.50"), IMAGE_URL, EXPECTATION));
		dog.select();
		dog.changePersonalities(Set.of(Personality.FRIENDLY));
		return dogRepository.saveAndFlush(dog);
	}
}
