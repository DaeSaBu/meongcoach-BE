package com.daesabu.meongcoach.dog.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import com.daesabu.meongcoach.dog.domain.shared.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.shared.Personality;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * 강아지 리포지토리 검증.
 */
@DataJpaTest
@DisplayName("강아지 리포지토리")
class DogRepositoryTest {

	private static final Long USER_ID = 42L;
	private static final Long OTHER_USER_ID = 99L;
	private static final String IMAGE_URL = "https://images.test.meongcoach.com/images/dog-profile/42/a.jpg";

	@Autowired
	private DogRepository dogRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("사용자의 선택된 강아지를 조회한다")
	void findFirstByUserIdAndStatusReturnsSelectedDog() {
		persistUnselectedDog(USER_ID);
		Dog selected = persistSelectedDog(USER_ID);

		Optional<Dog> found = dogRepository.findFirstByUserIdAndStatusOrderByIdAsc(USER_ID, DogStatus.SELECTED);

		assertThat(found.orElseThrow().getId()).isEqualTo(selected.getId());
	}

	// 애플리케이션 레이어에서만 불변식을 강제하므로, 선택이 여러 건인 데이터가 남아 있어도 예외 없이 끝나야 한다
	@Test
	@DisplayName("선택된 강아지가 여러 건이어도 가장 먼저 등록된 한 마리를 반환한다")
	void findFirstByUserIdAndStatusReturnsEarliestWhenSelectionIsDuplicated() {
		Dog first = persistSelectedDog(USER_ID);
		persistSelectedDog(USER_ID);

		Optional<Dog> found = dogRepository.findFirstByUserIdAndStatusOrderByIdAsc(USER_ID, DogStatus.SELECTED);

		assertThat(found.orElseThrow().getId()).isEqualTo(first.getId());
	}

	@Test
	@DisplayName("선택된 강아지가 없으면 빈 Optional을 반환한다")
	void findFirstByUserIdAndStatusReturnsEmptyWhenNoSelectedDogExists() {
		persistUnselectedDog(USER_ID);

		assertThat(dogRepository.findFirstByUserIdAndStatusOrderByIdAsc(USER_ID, DogStatus.SELECTED)).isEmpty();
	}

	@Test
	@DisplayName("사용자의 강아지를 등록 순으로 모두 조회한다")
	void findAllByUserIdOrderByIdAscReturnsDogsInRegistrationOrder() {
		Dog first = persistSelectedDog(USER_ID);
		Dog second = persistUnselectedDog(USER_ID);
		Dog third = persistUnselectedDog(USER_ID);

		List<Dog> found = dogRepository.findAllByUserIdOrderByIdAsc(USER_ID);

		assertThat(found).extracting(Dog::getId)
				.containsExactly(first.getId(), second.getId(), third.getId());
	}

	@Test
	@DisplayName("다른 사용자의 강아지는 포함하지 않는다")
	void findAllByUserIdOrderByIdAscIgnoresOtherUsersDog() {
		Dog mine = persistSelectedDog(USER_ID);
		persistSelectedDog(OTHER_USER_ID);

		List<Dog> found = dogRepository.findAllByUserIdOrderByIdAsc(USER_ID);

		assertThat(found).extracting(Dog::getId).containsExactly(mine.getId());
	}

	@Test
	@DisplayName("강아지가 없으면 빈 리스트를 반환한다")
	void findAllByUserIdOrderByIdAscReturnsEmptyWhenNoDogExists() {
		assertThat(dogRepository.findAllByUserIdOrderByIdAsc(USER_ID)).isEmpty();
	}

	// 성격 컬렉션은 LAZY라 트랜잭션 밖(adapter)에서 읽기 전에 선로딩돼야 한다. 영속성 컨텍스트를 비워 실제 쿼리 결과만으로 확인한다
	@Test
	@DisplayName("목록 조회는 성격을 함께 로딩하고 성격이 여러 개여도 강아지를 중복시키지 않는다")
	void findAllByUserIdOrderByIdAscFetchesPersonalitiesWithoutDuplicatingDogs() {
		Dog dog = persistDogWithPersonalities(USER_ID, Set.of("TIMID", "LIVELY"));
		entityManager.clear();

		List<Dog> found = dogRepository.findAllByUserIdOrderByIdAsc(USER_ID);

		assertThat(found).extracting(Dog::getId).containsExactly(dog.getId());
		assertThat(found.getFirst().getPersonalities())
				.containsExactlyInAnyOrder(Personality.TIMID, Personality.LIVELY);
	}

	@Test
	@DisplayName("ID와 소유자가 모두 일치하는 강아지를 성격과 함께 조회한다")
	void findByIdAndUserIdReturnsOwnedDogWithPersonalities() {
		Dog dog = persistDogWithPersonalities(USER_ID, Set.of("FRIENDLY"));
		entityManager.clear();

		Optional<Dog> found = dogRepository.findByIdAndUserId(dog.getId(), USER_ID);

		assertThat(found.orElseThrow().getId()).isEqualTo(dog.getId());
		assertThat(found.orElseThrow().getPersonalities()).containsExactly(Personality.FRIENDLY);
	}

	@Test
	@DisplayName("다른 사용자의 강아지 ID로 조회하면 빈 Optional을 반환한다")
	void findByIdAndUserIdReturnsEmptyForOtherUsersDog() {
		Dog othersDog = persistSelectedDog(OTHER_USER_ID);

		assertThat(dogRepository.findByIdAndUserId(othersDog.getId(), USER_ID)).isEmpty();
	}

	@Test
	@DisplayName("없는 강아지 ID로 조회하면 빈 Optional을 반환한다")
	void findByIdAndUserIdReturnsEmptyWhenDogDoesNotExist() {
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

	private Dog persistDogWithPersonalities(Long userId, Set<String> personalities) {
		Dog dog = newDog(userId, personalities);
		return dogRepository.saveAndFlush(dog);
	}

	private Dog newDog(Long userId) {
		return newDog(userId, null);
	}

	private Dog newDog(Long userId, Set<String> personalities) {
		return Dog.register(userId, new DogRegisterCommand("초코", "POODLE", "MALE",
				LocalDate.of(2024, 3, 1), new BigDecimal("4.50"), personalities, IMAGE_URL, null));
	}
}
