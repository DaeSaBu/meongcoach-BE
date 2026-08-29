package com.daesabu.meongcoach.dog.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.dog.domain.Breed;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import com.daesabu.meongcoach.dog.domain.Personality;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * 강아지 리포지토리 검증.
 */
@DataJpaTest
class DogRepositoryTest {

	private static final Long USER_ID = 42L;
	private static final Long OTHER_USER_ID = 99L;
	private static final String IMAGE_URL = "https://images.test.meongcoach.com/images/dog-profile/42/a.jpg";

	@Autowired
	private DogRepository dogRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 사용자의_선택된_강아지를_조회한다() {
		persistUnselectedDog(USER_ID);
		Dog selected = persistSelectedDog(USER_ID);

		Optional<Dog> found = dogRepository.findFirstByUserIdAndStatusOrderByIdAsc(USER_ID, DogStatus.SELECTED);

		assertThat(found.orElseThrow().getId()).isEqualTo(selected.getId());
	}

	// 애플리케이션 레이어에서만 불변식을 강제하므로, 선택이 여러 건인 데이터가 남아 있어도 예외 없이 끝나야 한다
	@Test
	void 선택된_강아지가_여러_건이어도_가장_먼저_등록된_한_마리를_반환한다() {
		Dog first = persistSelectedDog(USER_ID);
		persistSelectedDog(USER_ID);

		Optional<Dog> found = dogRepository.findFirstByUserIdAndStatusOrderByIdAsc(USER_ID, DogStatus.SELECTED);

		assertThat(found.orElseThrow().getId()).isEqualTo(first.getId());
	}

	@Test
	void 선택된_강아지가_없으면_빈_Optional을_반환한다() {
		persistUnselectedDog(USER_ID);

		assertThat(dogRepository.findFirstByUserIdAndStatusOrderByIdAsc(USER_ID, DogStatus.SELECTED)).isEmpty();
	}

	@Test
	void 사용자의_강아지를_등록_순으로_모두_조회한다() {
		Dog first = persistSelectedDog(USER_ID);
		Dog second = persistUnselectedDog(USER_ID);
		Dog third = persistUnselectedDog(USER_ID);

		List<Dog> found = dogRepository.findAllByUserIdOrderByIdAsc(USER_ID);

		assertThat(found).extracting(Dog::getId)
				.containsExactly(first.getId(), second.getId(), third.getId());
	}

	@Test
	void 다른_사용자의_강아지는_포함하지_않는다() {
		Dog mine = persistSelectedDog(USER_ID);
		persistSelectedDog(OTHER_USER_ID);

		List<Dog> found = dogRepository.findAllByUserIdOrderByIdAsc(USER_ID);

		assertThat(found).extracting(Dog::getId).containsExactly(mine.getId());
	}

	@Test
	void 강아지가_없으면_빈_리스트를_반환한다() {
		assertThat(dogRepository.findAllByUserIdOrderByIdAsc(USER_ID)).isEmpty();
	}

	// 성격 컬렉션은 LAZY라 트랜잭션 밖(adapter)에서 읽기 전에 선로딩돼야 한다. 영속성 컨텍스트를 비워 실제 쿼리 결과만으로 확인한다
	@Test
	void 목록_조회는_성격을_함께_로딩하고_성격이_여러_개여도_강아지를_중복시키지_않는다() {
		Dog dog = persistDogWithPersonalities(USER_ID, Set.of(Personality.TIMID, Personality.LIVELY));
		entityManager.clear();

		List<Dog> found = dogRepository.findAllByUserIdOrderByIdAsc(USER_ID);

		assertThat(found).extracting(Dog::getId).containsExactly(dog.getId());
		assertThat(found.getFirst().getPersonalities())
				.containsExactlyInAnyOrder(Personality.TIMID, Personality.LIVELY);
	}

	@Test
	void ID와_소유자가_모두_일치하는_강아지를_성격과_함께_조회한다() {
		Dog dog = persistDogWithPersonalities(USER_ID, Set.of(Personality.FRIENDLY));
		entityManager.clear();

		Optional<Dog> found = dogRepository.findByIdAndUserId(dog.getId(), USER_ID);

		assertThat(found.orElseThrow().getId()).isEqualTo(dog.getId());
		assertThat(found.orElseThrow().getPersonalities()).containsExactly(Personality.FRIENDLY);
	}

	@Test
	void 다른_사용자의_강아지_ID로_조회하면_빈_Optional을_반환한다() {
		Dog othersDog = persistSelectedDog(OTHER_USER_ID);

		assertThat(dogRepository.findByIdAndUserId(othersDog.getId(), USER_ID)).isEmpty();
	}

	@Test
	void 없는_강아지_ID로_조회하면_빈_Optional을_반환한다() {
		assertThat(dogRepository.findByIdAndUserId(999L, USER_ID)).isEmpty();
	}

	private Dog persistSelectedDog(Long userId) {
		Dog dog = newDog(userId);
		dog.select();
		return dogRepository.saveAndFlush(dog);
	}

	private Dog persistUnselectedDog(Long userId) {
		return dogRepository.saveAndFlush(newDog(userId));
	}

	private Dog persistDogWithPersonalities(Long userId, Set<Personality> personalities) {
		Dog dog = newDog(userId);
		dog.changePersonalities(personalities);
		return dogRepository.saveAndFlush(dog);
	}

	private Dog newDog(Long userId) {
		return Dog.register(new DogRegisterCommand(userId, "초코", Breed.POODLE, DogSex.MALE,
				LocalDate.of(2024, 3, 1), new BigDecimal("4.50"), IMAGE_URL, null));
	}
}
