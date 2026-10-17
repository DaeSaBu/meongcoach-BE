package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CurriculumTest {

	@Test
	void 레슨_id를_레슨_순서대로_반환한다() {
		Curriculum curriculum = curriculumWithLessonIds(3L, 1L, 2L);

		List<Long> lessonIds = curriculum.getLessonIds();

		assertThat(lessonIds).containsExactly(3L, 1L, 2L);
	}

	@Test
	void 완료한_레슨_id에_속한_레슨만_센다() {
		Curriculum curriculum = curriculumWithLessonIds(1L, 2L, 3L);

		int completedLessons = curriculum.countCompletedLessons(Set.of(1L, 3L));

		assertThat(completedLessons).isEqualTo(2);
	}

	@Test
	void 다른_커리큘럼의_레슨_id는_세지_않는다() {
		Curriculum curriculum = curriculumWithLessonIds(1L, 2L);

		int completedLessons = curriculum.countCompletedLessons(Set.of(2L, 99L));

		assertThat(completedLessons).isEqualTo(1);
	}

	@Test
	void 레슨이_없으면_완료한_레슨은_0개다() {
		Curriculum curriculum = curriculumWithLessonIds();

		int completedLessons = curriculum.countCompletedLessons(Set.of(1L));

		assertThat(completedLessons).isZero();
	}

	@Test
	void 레슨이_없으면_시작_전_상태다() {
		Curriculum curriculum = curriculumWithLessonIds();

		CurriculumStatus status = curriculum.statusOf(Set.of());

		assertThat(status).isEqualTo(CurriculumStatus.NOT_STARTED);
	}

	@Test
	void 일부_레슨만_완료하면_진행_중_상태다() {
		Curriculum curriculum = curriculumWithLessonIds(1L, 2L);

		CurriculumStatus status = curriculum.statusOf(Set.of(1L));

		assertThat(status).isEqualTo(CurriculumStatus.IN_PROGRESS);
	}

	@Test
	void 모든_레슨을_완료하면_완료_상태다() {
		Curriculum curriculum = curriculumWithLessonIds(1L, 2L);

		CurriculumStatus status = curriculum.statusOf(Set.of(1L, 2L));

		assertThat(status).isEqualTo(CurriculumStatus.COMPLETED);
	}

	@Test
	void 유료_커리큘럼은_카테고리의_이용권을_요구한다() {
		Curriculum curriculum = curriculumIn(EntitlementType.PUPPY, true);

		assertThat(curriculum.findRequiredEntitlementType()).contains(EntitlementType.PUPPY);
	}

	@Test
	void 맛보기_커리큘럼은_이용권을_요구하지_않는다() {
		Curriculum curriculum = curriculumIn(EntitlementType.PUPPY, false);

		assertThat(curriculum.findRequiredEntitlementType()).isEmpty();
	}

	@Test
	void 이용권이_없는_카테고리의_커리큘럼은_유료여도_이용권을_요구하지_않는다() {
		Curriculum curriculum = curriculumIn(null, true);

		assertThat(curriculum.findRequiredEntitlementType()).isEmpty();
	}

	@Test
	void 이용권을_요구하는_커리큘럼은_이용권이_없으면_잠긴다() {
		Curriculum curriculum = curriculumIn(EntitlementType.PUPPY, true);

		assertThat(curriculum.isLocked(false)).isTrue();
	}

	@Test
	void 이용권을_요구하는_커리큘럼도_이용권이_있으면_잠기지_않는다() {
		Curriculum curriculum = curriculumIn(EntitlementType.PUPPY, true);

		assertThat(curriculum.isLocked(true)).isFalse();
	}

	@Test
	void 이용권을_요구하지_않는_커리큘럼은_이용권이_없어도_잠기지_않는다() {
		Curriculum curriculum = curriculumIn(EntitlementType.PUPPY, false);

		assertThat(curriculum.isLocked(false)).isFalse();
	}

	private Curriculum curriculumIn(EntitlementType requiredEntitlementType, boolean isPremium) {
		TrainingCategory category = TrainingCategoryFixture.create("퍼피 교육", 1, null, null, requiredEntitlementType);
		Topic topic = TopicFixture.create(category, "앉아", 1, null, null, null);
		return CurriculumFixture.create(topic, "커리큘럼", 1, null, null, isPremium);
	}

	private Curriculum curriculumWithLessonIds(Long... lessonIds) {
		Curriculum curriculum = CurriculumFixture.create(null, "커리큘럼", 1, null, null);
		List<Lesson> lessons = new ArrayList<>();
		for (int i = 0; i < lessonIds.length; i++) {
			Lesson lesson = LessonFixture.create(curriculum, "레슨", i + 1, 5);
			ReflectionTestUtils.setField(lesson, "id", lessonIds[i]);
			lessons.add(lesson);
		}
		ReflectionTestUtils.setField(curriculum, "lessons", lessons);
		return curriculum;
	}
}
