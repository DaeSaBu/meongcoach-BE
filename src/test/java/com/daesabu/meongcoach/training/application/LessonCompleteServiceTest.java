package com.daesabu.meongcoach.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.progress.application.LessonProgressService;
import com.daesabu.meongcoach.training.application.provided.LessonCompleter;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumCreateCommand;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonCreateCommand;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicCreateCommand;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.exception.LessonNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * 레슨 완료 서비스 검증.
 */
@DataJpaTest
@Import({LessonCompleteService.class, LessonProgressService.class})
@DisplayName("레슨 완료 서비스")
class LessonCompleteServiceTest {

	private static final Long USER_ID = 1L;

	private static final Long OTHER_USER_ID = 2L;

	private static final Long ABSENT_LESSON_ID = 999L;

	@Autowired
	private LessonCompleter lessonCompleter;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("첫 완료 시 완료 횟수 1을 반환한다")
	void completeLessonReturnsOneOnFirstCompletion() {
		Lesson lesson = persistLesson("앉아");
		flushAndClear();

		int completedCount = lessonCompleter.completeLesson(USER_ID, lesson.getId());

		assertThat(completedCount).isOne();
	}

	@Test
	@DisplayName("첫 완료 시 진행도가 생성되고 완료 횟수가 1이 된다")
	void completeLessonCreatesProgressOnFirstCompletion() {
		Lesson lesson = persistLesson("앉아");
		flushAndClear();

		lessonCompleter.completeLesson(USER_ID, lesson.getId());

		flushAndClear();
		assertThat(countProgress()).isOne();
		assertThat(findCompletedCount(USER_ID, lesson.getId())).isOne();
	}

	@Test
	@DisplayName("반복 완료 시 호출할 때마다 완료 횟수가 1씩 증가한다")
	void completeLessonIncreasesCompletedCountOnEachCall() {
		Lesson lesson = persistLesson("앉아");
		flushAndClear();

		assertThat(lessonCompleter.completeLesson(USER_ID, lesson.getId())).isEqualTo(1);
		assertThat(lessonCompleter.completeLesson(USER_ID, lesson.getId())).isEqualTo(2);
		assertThat(lessonCompleter.completeLesson(USER_ID, lesson.getId())).isEqualTo(3);

		flushAndClear();
		assertThat(countProgress()).isOne();
		assertThat(findCompletedCount(USER_ID, lesson.getId())).isEqualTo(3);
	}

	@Test
	@DisplayName("다른 사용자의 완료 횟수에는 영향을 주지 않는다")
	void completeLessonDoesNotAffectOtherUserProgress() {
		Lesson lesson = persistLesson("앉아");
		flushAndClear();
		lessonCompleter.completeLesson(OTHER_USER_ID, lesson.getId());
		lessonCompleter.completeLesson(OTHER_USER_ID, lesson.getId());

		int completedCount = lessonCompleter.completeLesson(USER_ID, lesson.getId());

		flushAndClear();
		assertThat(completedCount).isOne();
		assertThat(findCompletedCount(USER_ID, lesson.getId())).isOne();
		assertThat(findCompletedCount(OTHER_USER_ID, lesson.getId())).isEqualTo(2);
	}

	@Test
	@DisplayName("존재하지 않는 레슨이면 예외를 던진다")
	void completeLessonThrowsWhenLessonDoesNotExist() {
		assertThatThrownBy(() -> lessonCompleter.completeLesson(USER_ID, ABSENT_LESSON_ID))
				.isInstanceOf(LessonNotFoundException.class);
	}

	@Test
	@DisplayName("존재하지 않는 레슨이면 진행도를 변경하지 않는다")
	void completeLessonDoesNotChangeProgressWhenLessonDoesNotExist() {
		Lesson lesson = persistLesson("앉아");
		flushAndClear();
		lessonCompleter.completeLesson(USER_ID, lesson.getId());
		flushAndClear();

		assertThatThrownBy(() -> lessonCompleter.completeLesson(USER_ID, ABSENT_LESSON_ID))
				.isInstanceOf(LessonNotFoundException.class);

		flushAndClear();
		assertThat(countProgress()).isOne();
		assertThat(findCompletedCount(USER_ID, lesson.getId())).isOne();
	}

	@Test
	@DisplayName("완료 원장은 기록하지 않는다")
	void completeLessonDoesNotRecordCompletionLog() {
		Lesson lesson = persistLesson("앉아");
		flushAndClear();

		lessonCompleter.completeLesson(USER_ID, lesson.getId());

		flushAndClear();
		assertThat(countCompletionLogs()).isZero();
	}

	private Lesson persistLesson(String title) {
		TrainingCategory category = entityManager.persist(TrainingCategory.create(title + " 카테고리", 1, null, null));
		Topic topic = entityManager.persist(Topic.create(category, new TopicCreateCommand(title, 1, null, null, null)));
		CurriculumCreateCommand curriculumCommand = new CurriculumCreateCommand(title + " 커리큘럼", 1, null, null);
		Curriculum curriculum = entityManager.persist(Curriculum.create(topic, curriculumCommand));
		return entityManager.persist(Lesson.create(curriculum, new LessonCreateCommand(title + " 레슨", 1, 5)));
	}

	private long countProgress() {
		return entityManager.getEntityManager()
				.createQuery("select count(p) from UserLessonProgress p", Long.class)
				.getSingleResult();
	}

	private long countCompletionLogs() {
		return entityManager.getEntityManager()
				.createQuery("select count(l) from LessonCompletionLog l", Long.class)
				.getSingleResult();
	}

	private int findCompletedCount(Long userId, Long lessonId) {
		return entityManager.getEntityManager()
				.createQuery("select p.completedCount from UserLessonProgress p "
						+ "where p.userId = :userId and p.lessonId = :lessonId", Integer.class)
				.setParameter("userId", userId)
				.setParameter("lessonId", lessonId)
				.getSingleResult();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
