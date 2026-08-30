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
class LessonCompleteServiceTest {

	private static final Long USER_ID = 1L;

	private static final Long OTHER_USER_ID = 2L;

	private static final Long ABSENT_LESSON_ID = 999L;

	@Autowired
	private LessonCompleter lessonCompleter;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 첫_완료_시_완료_횟수_1을_반환한다() {
		Lesson lesson = persistLesson("앉아");
		flushAndClear();

		int completedCount = lessonCompleter.completeLesson(USER_ID, lesson.getId());

		assertThat(completedCount).isOne();
	}

	@Test
	void 첫_완료_시_진행도가_생성되고_완료_횟수가_1이_된다() {
		Lesson lesson = persistLesson("앉아");
		flushAndClear();

		lessonCompleter.completeLesson(USER_ID, lesson.getId());

		flushAndClear();
		assertThat(countProgress()).isOne();
		assertThat(findCompletedCount(USER_ID, lesson.getId())).isOne();
	}

	@Test
	void 반복_완료_시_호출할_때마다_완료_횟수가_1씩_증가한다() {
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
	void 다른_사용자의_완료_횟수에는_영향을_주지_않는다() {
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
	void 존재하지_않는_레슨이면_예외를_던진다() {
		assertThatThrownBy(() -> lessonCompleter.completeLesson(USER_ID, ABSENT_LESSON_ID))
				.isInstanceOf(LessonNotFoundException.class);
	}

	@Test
	void 존재하지_않는_레슨이면_진행도를_변경하지_않는다() {
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
