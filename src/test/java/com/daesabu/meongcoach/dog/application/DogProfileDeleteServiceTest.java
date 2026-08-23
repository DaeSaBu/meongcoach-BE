package com.daesabu.meongcoach.dog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.application.provided.DogProfileDeleter;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Breed;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import com.daesabu.meongcoach.dog.domain.Personality;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.dog.domain.exception.LastDogNotDeletableException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(DogProfileDeleteService.class)
class DogProfileDeleteServiceTest {

	private static final Long USER_ID = 42L;
	private static final Long OTHER_USER_ID = 99L;
	private static final String IMAGE_URL = "https://images.test.meongcoach.com/images/dog-profile/42/a.jpg";
	private static final String EXPECTATION = "보호자와 즐겁게 교육받고 싶어요.";

	@Autowired
	private DogProfileDeleter dogProfileDeleter;

	@Autowired
	private DogRepository dogRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 소유_강아지를_삭제하면_조회에서_제외된다() {
		Dog saved = persistSelectedDog(USER_ID);
		dogRepository.saveAndFlush(newDog(USER_ID));
		entityManager.clear();

		dogProfileDeleter.delete(USER_ID, saved.getId());
		entityManager.flush();
		entityManager.clear();

		assertThat(dogRepository.findByIdAndUserId(saved.getId(), USER_ID)).isEmpty();
		assertThat(dogRepository.findById(saved.getId())).isEmpty();
	}

	@Test
	void 삭제해도_행은_남고_삭제_시각이_기록된다() {
		Dog saved = persistSelectedDog(USER_ID);
		dogRepository.saveAndFlush(newDog(USER_ID));
		entityManager.clear();

		dogProfileDeleter.delete(USER_ID, saved.getId());
		entityManager.flush();
		entityManager.clear();

		// 엔티티 조회는 삭제 건을 자동 제외하므로 네이티브 쿼리로 행 존재와 삭제 시각을 확인한다
		Object deletedAt = entityManager.getEntityManager()
				.createNativeQuery("select deleted_at from dogs where id = :id")
				.setParameter("id", saved.getId())
				.getSingleResult();
		assertThat(deletedAt).isNotNull();
	}

	@Test
	void 선택된_강아지를_삭제해도_남은_강아지는_선택되지_않는다() {
		Dog selected = persistSelectedDog(USER_ID);
		Dog unselected = dogRepository.saveAndFlush(newDog(USER_ID));
		entityManager.clear();

		dogProfileDeleter.delete(USER_ID, selected.getId());
		entityManager.flush();
		entityManager.clear();

		assertThat(dogRepository.existsByUserIdAndStatus(USER_ID, DogStatus.SELECTED)).isFalse();
		Dog remaining = dogRepository.findById(unselected.getId()).orElseThrow();
		assertThat(remaining.getStatus()).isEqualTo(DogStatus.UNSELECTED);
	}

	@Test
	void 없는_강아지_ID로_삭제하면_예외를_던진다() {
		assertThatThrownBy(() -> dogProfileDeleter.delete(USER_ID, 999L))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	void 다른_사용자의_강아지는_삭제할_수_없다() {
		Dog othersDog = persistSelectedDog(OTHER_USER_ID);

		assertThatThrownBy(() -> dogProfileDeleter.delete(USER_ID, othersDog.getId()))
				.isInstanceOf(DogNotFoundException.class);

		entityManager.clear();
		assertThat(dogRepository.findById(othersDog.getId())).isPresent();
	}

	@Test
	void 이미_삭제된_강아지를_다시_삭제하면_예외를_던진다() {
		Dog saved = persistSelectedDog(USER_ID);
		dogRepository.saveAndFlush(newDog(USER_ID));
		dogProfileDeleter.delete(USER_ID, saved.getId());
		entityManager.flush();
		entityManager.clear();

		assertThatThrownBy(() -> dogProfileDeleter.delete(USER_ID, saved.getId()))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	void 마지막_한_마리는_삭제할_수_없다() {
		Dog only = persistSelectedDog(USER_ID);
		entityManager.clear();

		assertThatThrownBy(() -> dogProfileDeleter.delete(USER_ID, only.getId()))
				.isInstanceOf(LastDogNotDeletableException.class);

		entityManager.clear();
		Dog found = dogRepository.findById(only.getId()).orElseThrow();
		assertThat(found.getDeletedAt()).isNull();
	}

	@Test
	void 이미_삭제된_강아지는_개수에_포함되지_않는다() {
		Dog remaining = persistSelectedDog(USER_ID);
		Dog deleted = dogRepository.saveAndFlush(newDog(USER_ID));
		deleted.delete();
		dogRepository.saveAndFlush(deleted);
		entityManager.clear();

		assertThatThrownBy(() -> dogProfileDeleter.delete(USER_ID, remaining.getId()))
				.isInstanceOf(LastDogNotDeletableException.class);
	}

	private Dog persistSelectedDog(Long userId) {
		Dog dog = newDog(userId);
		dog.select();
		return dogRepository.saveAndFlush(dog);
	}

	private Dog newDog(Long userId) {
		Dog dog = Dog.register(new DogRegisterCommand(userId, "초코", Breed.POODLE, DogSex.MALE,
				LocalDate.of(2024, 3, 1), new BigDecimal("4.50"), IMAGE_URL, EXPECTATION));
		dog.changePersonalities(Set.of(Personality.FRIENDLY));
		return dog;
	}
}
