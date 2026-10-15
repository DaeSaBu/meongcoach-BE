package com.daesabu.meongcoach.dog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.DogProfileResult;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import com.daesabu.meongcoach.dog.domain.Dogs;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.Personality;
import com.daesabu.meongcoach.dog.domain.shared.RegisteredBreed;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(DogProfileFinderService.class)
class DogProfileFinderServiceTest {

	private static final Long USER_ID = 42L;
	private static final Long OTHER_USER_ID = 99L;
	private static final String IMAGE_URL = "https://images.test.meongcoach.com/images/dog-profile/42/a.jpg";
	private static final String EXPECTATION = "보호자와 즐겁게 교육받고 싶어요.";

	@Autowired
	private DogProfileFinder dogProfileFinder;

	@Autowired
	private DogRepository dogRepository;

	@Test
	void 사용자의_모든_강아지를_등록_순으로_반환한다() {
		Dog selected = persistSelectedDog(USER_ID);
		Dog unselected = dogRepository.saveAndFlush(newDog(USER_ID));

		List<Dog> found = dogProfileFinder.findDogs(USER_ID);

		assertThat(found).extracting(Dog::getId).containsExactly(selected.getId(), unselected.getId());
	}

	@Test
	void 강아지가_없으면_빈_리스트를_반환한다() {
		assertThat(dogProfileFinder.findDogs(USER_ID)).isEmpty();
	}

	@Test
	void 다른_사용자의_강아지는_반환하지_않는다() {
		persistSelectedDog(OTHER_USER_ID);

		assertThat(dogProfileFinder.findDogs(USER_ID)).isEmpty();
	}

	@Test
	void 사용자_소유_강아지를_ID로_조회해_프로필_정보를_반환한다() {
		Dog saved = persistSelectedDog(USER_ID);

		Dog found = dogProfileFinder.findDog(USER_ID, saved.getId());

		assertThat(found.getId()).isEqualTo(saved.getId());
		assertThat(found.getName()).isEqualTo("초코");
		assertThat(found.getBreed()).isEqualTo(Breed.of(RegisteredBreed.POODLE));
		assertThat(found.getSex()).isEqualTo(DogSex.MALE);
		assertThat(found.getBirthDate()).isEqualTo(LocalDate.of(2024, 3, 1));
		assertThat(found.getWeightKg()).isEqualByComparingTo(new BigDecimal("4.50"));
		assertThat(found.getStatus()).isEqualTo(DogStatus.SELECTED);
		assertThat(found.getProfileImageUrl()).isEqualTo(IMAGE_URL);
		assertThat(found.getExpectation()).isEqualTo(EXPECTATION);
		assertThat(found.getPersonalities()).containsExactly(Personality.FRIENDLY);
	}

	@Test
	void 프로필을_표시용_결과로_반환한다() {
		Dog saved = persistSelectedDog(USER_ID);

		DogProfileResult result = dogProfileFinder.findProfile(USER_ID, saved.getId());

		assertThat(result.id()).isEqualTo(saved.getId());
		assertThat(result.name()).isEqualTo("초코");
		assertThat(result.birthDate()).isEqualTo(LocalDate.of(2024, 3, 1));
		assertThat(result.birthDateEstimated()).isTrue();
		assertThat(result.ageInMonths()).isEqualTo(saved.getAgeInMonths());
		assertThat(result.breedLabel()).isEqualTo("푸들");
		assertThat(result.sex()).isEqualTo("MALE");
		assertThat(result.personalityLabels()).containsExactly("친화적");
		assertThat(result.weightKg()).isEqualByComparingTo("4.50");
		assertThat(result.expectation()).isEqualTo(EXPECTATION);
	}

	@Test
	void 생년월일과_몸무게가_없는_프로필은_해당_값이_null이다() {
		Dog dog = Dog.register(new DogRegisterCommand(USER_ID, "초코", Breed.of(RegisteredBreed.POODLE), DogSex.FEMALE, null, false, null,
				null, null));
		Dog saved = dogRepository.saveAndFlush(dog);

		DogProfileResult result = dogProfileFinder.findProfile(USER_ID, saved.getId());

		assertThat(result.birthDate()).isNull();
		assertThat(result.ageInMonths()).isNull();
		assertThat(result.weightKg()).isNull();
		assertThat(result.birthDateEstimated()).isFalse();
		assertThat(result.personalityLabels()).isEmpty();
		assertThat(result.expectation()).isEmpty();
	}

	@Test
	void 다른_사용자의_강아지_프로필을_조회하면_예외를_던진다() {
		Dog othersDog = persistSelectedDog(OTHER_USER_ID);

		assertThatThrownBy(() -> dogProfileFinder.findProfile(USER_ID, othersDog.getId()))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	void 없는_강아지_ID로_프로필을_조회하면_예외를_던진다() {
		assertThatThrownBy(() -> dogProfileFinder.findProfile(USER_ID, 999L))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	void 있는_강아지는_프로필을_Optional로_반환한다() {
		Dog saved = persistSelectedDog(USER_ID);

		Optional<DogProfileResult> result = dogProfileFinder.findProfileIfPresent(USER_ID, saved.getId());

		assertThat(result).isPresent();
		assertThat(result.get().id()).isEqualTo(saved.getId());
		assertThat(result.get().name()).isEqualTo("초코");
	}

	@Test
	void 없거나_다른_사용자의_강아지는_예외_없이_빈_값을_반환한다() {
		Dog othersDog = persistSelectedDog(OTHER_USER_ID);

		assertThat(dogProfileFinder.findProfileIfPresent(USER_ID, othersDog.getId())).isEmpty();
		assertThat(dogProfileFinder.findProfileIfPresent(USER_ID, 999L)).isEmpty();
	}

	@Test
	void 다른_사용자의_강아지_ID로_조회하면_예외를_던진다() {
		Dog othersDog = persistSelectedDog(OTHER_USER_ID);

		assertThatThrownBy(() -> dogProfileFinder.findDog(USER_ID, othersDog.getId()))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	void 없는_강아지_ID로_조회하면_예외를_던진다() {
		assertThatThrownBy(() -> dogProfileFinder.findDog(USER_ID, 999L))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	void 선택된_강아지를_반환한다() {
		Dog saved = persistSelectedDog(USER_ID);

		Dog found = dogProfileFinder.findSelectedDog(USER_ID);

		assertThat(found.getId()).isEqualTo(saved.getId());
		assertThat(found.getProfileImageUrl()).isEqualTo(IMAGE_URL);
	}

	@Test
	void 미선택_강아지와_함께_있어도_선택된_강아지를_반환한다() {
		dogRepository.saveAndFlush(newDog(USER_ID));
		Dog selected = persistSelectedDog(USER_ID);

		Dog found = dogProfileFinder.findSelectedDog(USER_ID);

		assertThat(found.getId()).isEqualTo(selected.getId());
	}

	@Test
	void 선택된_강아지의_프로필_이미지가_없으면_빈_문자열을_가진다() {
		Dog dog = Dog.register(new DogRegisterCommand(USER_ID, "초코", Breed.of(RegisteredBreed.POODLE), DogSex.MALE,
				LocalDate.of(2024, 3, 1), false, new BigDecimal("4.50"), null, null));
		dog.select();
		dogRepository.saveAndFlush(dog);

		Dog found = dogProfileFinder.findSelectedDog(USER_ID);

		assertThat(found.getProfileImageUrl()).isEmpty();
	}

	@Test
	void 미선택_강아지만_있으면_예외를_던진다() {
		dogRepository.saveAndFlush(newDog(USER_ID));

		assertThatThrownBy(() -> dogProfileFinder.findSelectedDog(USER_ID))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	void 강아지가_한_마리도_없으면_예외를_던진다() {
		assertThatThrownBy(() -> dogProfileFinder.findSelectedDog(USER_ID))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	void 다른_사용자의_선택된_강아지는_반환하지_않는다() {
		persistSelectedDog(OTHER_USER_ID);

		assertThatThrownBy(() -> dogProfileFinder.findSelectedDog(USER_ID))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	void 삭제된_강아지는_목록_조회에서_제외된다() {
		Dog remaining = persistSelectedDog(USER_ID);
		Dog deleted = dogRepository.saveAndFlush(newDog(USER_ID));
		new Dogs(List.of(remaining, deleted)).delete(deleted.getId());
		dogRepository.saveAndFlush(deleted);

		List<Dog> found = dogProfileFinder.findDogs(USER_ID);

		assertThat(found).extracting(Dog::getId).containsExactly(remaining.getId());
	}

	@Test
	void 삭제된_강아지는_단건_조회되지_않는다() {
		Dog remaining = dogRepository.saveAndFlush(newDog(USER_ID));
		Dog deleted = persistSelectedDog(USER_ID);
		new Dogs(List.of(remaining, deleted)).delete(deleted.getId());
		dogRepository.saveAndFlush(deleted);

		assertThatThrownBy(() -> dogProfileFinder.findDog(USER_ID, deleted.getId()))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	void 삭제된_강아지는_선택_조회에서_제외된다() {
		Dog remaining = dogRepository.saveAndFlush(newDog(USER_ID));
		Dog deleted = persistSelectedDog(USER_ID);
		new Dogs(List.of(remaining, deleted)).delete(deleted.getId());
		dogRepository.saveAndFlush(deleted);

		assertThatThrownBy(() -> dogProfileFinder.findSelectedDog(USER_ID))
				.isInstanceOf(DogNotFoundException.class);
	}

	private Dog persistSelectedDog(Long userId) {
		Dog dog = newDog(userId);
		dog.select();
		return dogRepository.saveAndFlush(dog);
	}

	private Dog newDog(Long userId) {
		Dog dog = Dog.register(new DogRegisterCommand(userId, "초코", Breed.of(RegisteredBreed.POODLE), DogSex.MALE,
				LocalDate.of(2024, 3, 1), true, new BigDecimal("4.50"), IMAGE_URL, EXPECTATION));
		dog.changePersonalities(Set.of(Personality.FRIENDLY));
		return dog;
	}
}
