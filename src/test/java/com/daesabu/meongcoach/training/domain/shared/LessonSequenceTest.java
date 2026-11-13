package com.daesabu.meongcoach.training.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LessonSequenceTest {

	@Test
	void 각_토픽의_첫_미완료_레슨을_독립적으로_허용한다() {
		LessonSequence sequence = new LessonSequence(List.of(List.of(101L, 102L), List.of(201L, 202L)));

		assertThat(sequence.canComplete(101L, Set.of())).isTrue();
		assertThat(sequence.canComplete(201L, Set.of())).isTrue();
		assertThat(sequence.canComplete(102L, Set.of())).isFalse();
		assertThat(sequence.canComplete(202L, Set.of())).isFalse();
		assertThat(sequence.canComplete(202L, Set.of(201L))).isTrue();
		assertThat(sequence.canComplete(102L, Set.of(201L))).isFalse();
	}

	@Test
	void 토픽의_앞_레슨을_완료해야_다음_레슨을_허용한다() {
		LessonSequence sequence = new LessonSequence(List.of(List.of(103L, 101L), List.of(102L)));

		assertThat(sequence.canComplete(101L, Set.of())).isFalse();
		assertThat(sequence.canComplete(101L, Set.of(103L))).isTrue();
		assertThat(sequence.canComplete(102L, Set.of(103L))).isTrue();
		assertThat(sequence.canComplete(103L, Set.of(103L))).isFalse();
	}

	@Test
	void 학습_순서는_호출자가_전달한_완료_기록으로_판단한다() {
		LessonSequence sequence = new LessonSequence(List.of(List.of(101L, 102L)));

		assertThat(sequence.canComplete(102L, Set.of(101L))).isTrue();
		assertThat(sequence.canComplete(102L, Set.of())).isFalse();
		assertThat(sequence.canComplete(101L, Set.of())).isTrue();
	}

	@Test
	void 교육_목록_앞에_새_레슨이_추가되면_그_레슨부터_진행한다() {
		LessonSequence sequence = new LessonSequence(List.of(List.of(104L, 102L, 103L)));

		assertThat(sequence.canComplete(104L, Set.of(101L, 102L))).isTrue();
		assertThat(sequence.canComplete(103L, Set.of(101L, 102L))).isFalse();
		assertThat(sequence.canComplete(103L, Set.of(101L, 102L, 104L))).isTrue();
	}

	@Test
	void 목록에_없는_레슨과_이미_완료한_레슨은_허용하지_않는다() {
		LessonSequence sequence = new LessonSequence(List.of(List.of(101L)));

		assertThat(sequence.canComplete(999L, Set.of())).isFalse();
		assertThat(sequence.canComplete(101L, Set.of(101L))).isFalse();
	}

	@Test
	void 레슨이_없으면_완료할_레슨도_없다() {
		LessonSequence sequence = new LessonSequence(List.of());

		assertThat(sequence.lessonIds()).isEmpty();
		assertThat(sequence.canComplete(101L, Set.of())).isFalse();
	}

	@Test
	void 원본_목록을_수정해도_학습_순서와_전체_레슨은_유지한다() {
		List<Long> lessons = new ArrayList<>(List.of(101L, 102L));
		List<List<Long>> topics = new ArrayList<>(List.of(lessons));
		LessonSequence sequence = new LessonSequence(topics);

		lessons.addFirst(100L);
		topics.clear();

		assertThat(sequence.lessonIds()).containsExactly(101L, 102L);
		assertThat(sequence.canComplete(101L, Set.of())).isTrue();
	}
}
