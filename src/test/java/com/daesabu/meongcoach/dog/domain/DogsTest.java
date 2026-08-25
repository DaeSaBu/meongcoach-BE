package com.daesabu.meongcoach.dog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.domain.exception.DogLimitExceededException;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.dog.domain.exception.LastDogNotDeletableException;
import com.daesabu.meongcoach.dog.domain.shared.DogRegisterCommand;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DogsTest {

	private static final DogRegisterCommand COMMAND = new DogRegisterCommand("초코", "POODLE", "MALE",
			LocalDate.of(2024, 3, 1), new BigDecimal("4.50"), null, null, null);

	@Test
	void 강아지가_5마리면_더_등록할_수_없다() {
		Dogs dogs = new Dogs(unselectedDogs(5));

		assertThatThrownBy(() -> dogs.register(1L, COMMAND))
				.isInstanceOf(DogLimitExceededException.class);
	}

	@Test
	void 강아지가_4마리면_등록할_수_있다() {
		Dogs dogs = new Dogs(unselectedDogs(4));

		Dog registered = dogs.register(1L, COMMAND);

		assertThat(registered.getName()).isEqualTo("초코");
	}

	@Test
	void 강아지가_없으면_등록하는_강아지가_선택된다() {
		Dogs dogs = new Dogs(List.of());

		Dog registered = dogs.register(1L, COMMAND);

		assertThat(registered.getStatus()).isEqualTo(DogStatus.SELECTED);
	}

	@Test
	void 선택된_강아지가_없으면_등록하는_강아지가_선택된다() {
		Dogs dogs = new Dogs(unselectedDogs(2));

		Dog registered = dogs.register(1L, COMMAND);

		assertThat(registered.getStatus()).isEqualTo(DogStatus.SELECTED);
	}

	@Test
	void 선택된_강아지가_있으면_등록하는_강아지는_미선택이다() {
		Dogs dogs = new Dogs(List.of(selectedDog(10L), unselectedDog(11L)));

		Dog registered = dogs.register(1L, COMMAND);

		assertThat(registered.getStatus()).isEqualTo(DogStatus.UNSELECTED);
	}

	@Test
	void 등록해도_넘겨준_목록은_바뀌지_않는다() {
		List<Dog> owned = List.of(selectedDog(10L));
		Dogs dogs = new Dogs(owned);

		dogs.register(1L, COMMAND);

		assertThat(owned).hasSize(1);
	}

	@Test
	void 마지막_한_마리는_삭제할_수_없다() {
		Dog only = selectedDog(10L);
		Dogs dogs = new Dogs(List.of(only));

		assertThatThrownBy(() -> dogs.delete(10L))
				.isInstanceOf(LastDogNotDeletableException.class);

		assertThat(only.getDeletedAt()).isNull();
	}

	@Test
	void 두_마리_이상이면_삭제하면_삭제_시각이_기록된다() {
		Dog target = selectedDog(10L);
		Dog other = unselectedDog(11L);
		Dogs dogs = new Dogs(List.of(target, other));

		dogs.delete(10L);

		assertThat(target.getDeletedAt()).isNotNull();
		assertThat(other.getDeletedAt()).isNull();
	}

	@Test
	void 없는_강아지를_삭제하면_예외를_던진다() {
		Dogs dogs = new Dogs(List.of(selectedDog(10L)));

		assertThatThrownBy(() -> dogs.delete(999L))
				.isInstanceOf(DogNotFoundException.class);
	}

	private List<Dog> unselectedDogs(int count) {
		return LongStream.range(0, count)
				.mapToObj(this::unselectedDog)
				.toList();
	}

	private Dog selectedDog(Long id) {
		Dog dog = unselectedDog(id);
		dog.select();
		return dog;
	}

	// 순수 단위 테스트라 JPA가 id를 채우지 않으므로, 삭제 대상 조회를 검증하려면 리플렉션으로 주입한다
	private Dog unselectedDog(Long id) {
		Dog dog = Dog.register(1L, COMMAND);
		ReflectionTestUtils.setField(dog, "id", id);
		return dog;
	}
}
