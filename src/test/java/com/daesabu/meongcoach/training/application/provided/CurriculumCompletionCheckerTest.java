package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumFixture;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonFixture;
import com.daesabu.meongcoach.training.domain.LessonProgress;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import com.daesabu.meongcoach.training.domain.exception.CurriculumNotFoundException;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class CurriculumCompletionCheckerTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private CurriculumCompletionChecker checker;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 지정한_커리큘럼의_모든_레슨을_완료하면_참이다() {
		Curriculum first = saveCurriculum();
		Curriculum second = saveCurriculum();
		complete(USER_ID, saveLesson(first));
		complete(USER_ID, saveLesson(first));
		complete(USER_ID, saveLesson(second));
		saveLesson(saveCurriculum());
		flushAndClear();

		assertThat(checker.areAllCompleted(USER_ID, List.of(second.getId(), first.getId()))).isTrue();
	}

	@Test
	void 미완료_레슨이_하나라도_남아_있으면_거짓이다() {
		Curriculum curriculum = saveCurriculum();
		complete(USER_ID, saveLesson(curriculum));
		saveLesson(curriculum);
		flushAndClear();

		assertThat(checker.areAllCompleted(USER_ID, List.of(curriculum.getId()))).isFalse();
	}

	@Test
	void 다른_사용자의_완료_기록은_포함하지_않는다() {
		Curriculum curriculum = saveCurriculum();
		complete(43L, saveLesson(curriculum));
		flushAndClear();

		assertThat(checker.areAllCompleted(USER_ID, List.of(curriculum.getId()))).isFalse();
	}

	@Test
	void 빈_목록은_거짓이다() {
		assertThat(checker.areAllCompleted(USER_ID, List.of())).isFalse();
	}

	@Test
	void 요청한_커리큘럼이_하나라도_없으면_예외를_던진다() {
		Curriculum curriculum = saveCurriculum();
		complete(USER_ID, saveLesson(curriculum));
		flushAndClear();

		assertThatThrownBy(() -> checker.areAllCompleted(USER_ID, List.of(curriculum.getId(), Long.MAX_VALUE)))
				.isInstanceOf(CurriculumNotFoundException.class);
	}

	@Test
	void 레슨이_없는_커리큘럼은_예외를_던진다() {
		Curriculum curriculum = saveCurriculum();
		flushAndClear();

		assertThatThrownBy(() -> checker.areAllCompleted(USER_ID, List.of(curriculum.getId())))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("커리큘럼에는 레슨이 하나 이상 있어야 합니다.");
	}

	@Test
	void 미완료_커리큘럼이_있어도_레슨_없는_커리큘럼을_검사한다() {
		Curriculum unfinished = saveCurriculum();
		saveLesson(unfinished);
		Curriculum empty = saveCurriculum();
		flushAndClear();

		assertThatThrownBy(() -> checker.areAllCompleted(USER_ID, List.of(unfinished.getId(), empty.getId())))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> checker.areAllCompleted(USER_ID, List.of(empty.getId(), unfinished.getId())))
				.isInstanceOf(IllegalStateException.class);
	}

	private Curriculum saveCurriculum() {
		TrainingCategory category = TrainingCategoryFixture.create("교육", 1, null, null);
		entityManager.persist(category);
		Topic topic = TopicFixture.create(category, "토픽", 1, null, null, null);
		entityManager.persist(topic);
		Curriculum curriculum = CurriculumFixture.create(topic, "커리큘럼", 1, null, null);
		entityManager.persist(curriculum);
		return curriculum;
	}

	private Lesson saveLesson(Curriculum curriculum) {
		Lesson lesson = LessonFixture.create(curriculum, "레슨", 1, 5);
		entityManager.persist(lesson);
		return lesson;
	}

	private void complete(Long userId, Lesson lesson) {
		LessonProgress progress = LessonProgress.start(userId, lesson.getId());
		progress.complete();
		entityManager.persist(progress);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
