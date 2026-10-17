package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.EntitlementFixture;
import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementRequiredException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.progress.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.progress.domain.LessonProgress;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.required.CurriculumRepository;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.application.required.TrainingCategoryRepository;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumFixture;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonFixture;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import com.daesabu.meongcoach.training.domain.exception.LessonNotFoundException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class LessonCompleterTest {

	private static final Long USER_ID = 1L;

	private static final Long OTHER_USER_ID = 2L;

	private static final Long ABSENT_LESSON_ID = 999L;

	@Autowired
	private LessonCompleter lessonCompleter;

	@Autowired
	private TrainingCategoryRepository trainingCategoryRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private CurriculumRepository curriculumRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private LessonProgressRepository lessonProgressRepository;

	@Autowired
	private EntitlementRepository entitlementRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 첫_완료_시_완료_횟수_1을_반환한다() {
		Lesson lesson = saveLesson("앉아");
		flushAndClear();

		int completedCount = lessonCompleter.completeLesson(USER_ID, lesson.getId());

		assertThat(completedCount).isOne();
	}

	@Test
	void 첫_완료_시_진행도가_생성되고_완료_횟수가_1이_된다() {
		Lesson lesson = saveLesson("앉아");
		flushAndClear();

		lessonCompleter.completeLesson(USER_ID, lesson.getId());

		flushAndClear();
		assertThat(lessonProgressRepository.count()).isOne();
		assertThat(findCompletedCount(USER_ID, lesson.getId())).isOne();
	}

	@Test
	void 반복_완료_시_호출할_때마다_완료_횟수가_1씩_증가한다() {
		Lesson lesson = saveLesson("앉아");
		flushAndClear();

		assertThat(lessonCompleter.completeLesson(USER_ID, lesson.getId())).isEqualTo(1);
		assertThat(lessonCompleter.completeLesson(USER_ID, lesson.getId())).isEqualTo(2);
		assertThat(lessonCompleter.completeLesson(USER_ID, lesson.getId())).isEqualTo(3);

		flushAndClear();
		assertThat(lessonProgressRepository.count()).isOne();
		assertThat(findCompletedCount(USER_ID, lesson.getId())).isEqualTo(3);
	}

	@Test
	void 다른_사용자의_완료_횟수에는_영향을_주지_않는다() {
		Lesson lesson = saveLesson("앉아");
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
		Lesson lesson = saveLesson("앉아");
		flushAndClear();
		lessonCompleter.completeLesson(USER_ID, lesson.getId());
		flushAndClear();

		assertThatThrownBy(() -> lessonCompleter.completeLesson(USER_ID, ABSENT_LESSON_ID))
				.isInstanceOf(LessonNotFoundException.class);

		flushAndClear();
		assertThat(lessonProgressRepository.count()).isOne();
		assertThat(findCompletedCount(USER_ID, lesson.getId())).isOne();
	}

	@Test
	void 이용권_없이_유료_레슨을_완료하면_예외를_던지고_진행도를_만들지_않는다() {
		Lesson lesson = savePremiumLesson("앉아", EntitlementType.PUPPY);
		flushAndClear();

		assertThatThrownBy(() -> lessonCompleter.completeLesson(USER_ID, lesson.getId()))
				.isInstanceOf(EntitlementRequiredException.class);

		flushAndClear();
		assertThat(lessonProgressRepository.count()).isZero();
	}

	@Test
	void 이용권이_있으면_유료_레슨을_완료한다() {
		Lesson lesson = savePremiumLesson("앉아", EntitlementType.PUPPY);
		entitlementRepository.save(EntitlementFixture.create(USER_ID, EntitlementType.PUPPY, null));
		flushAndClear();

		int completedCount = lessonCompleter.completeLesson(USER_ID, lesson.getId());

		assertThat(completedCount).isOne();
	}

	private Lesson saveLesson(String title) {
		return saveLesson(title, null, false);
	}

	private Lesson savePremiumLesson(String title, EntitlementType requiredEntitlementType) {
		return saveLesson(title, requiredEntitlementType, true);
	}

	private Lesson saveLesson(String title, EntitlementType requiredEntitlementType, boolean isPremium) {
		TrainingCategory category = trainingCategoryRepository.save(
				TrainingCategoryFixture.create(title + " 카테고리", 1, null, null, requiredEntitlementType));
		Topic topic = topicRepository.save(TopicFixture.create(category, title, 1, null, null, null));
		Curriculum curriculum = curriculumRepository.save(
				CurriculumFixture.create(topic, title + " 커리큘럼", 1, null, null, isPremium));
		return lessonRepository.save(LessonFixture.create(curriculum, title + " 레슨", 1, 5));
	}

	private int findCompletedCount(Long userId, Long lessonId) {
		return lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId)
				.map(LessonProgress::getCompletedCount)
				.orElseThrow();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
