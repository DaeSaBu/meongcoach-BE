package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.DogFixture;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.EntitlementFixture;
import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementRequiredException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.provided.dto.TopicSelectionRequest;
import com.daesabu.meongcoach.training.application.required.CurriculumProgressRepository;
import com.daesabu.meongcoach.training.application.required.CurriculumRepository;
import com.daesabu.meongcoach.training.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.application.required.TrainedLessonRepository;
import com.daesabu.meongcoach.training.application.required.TrainingCategoryRepository;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumFixture;
import com.daesabu.meongcoach.training.domain.CurriculumProgress;
import com.daesabu.meongcoach.training.domain.CurriculumProgressStatus;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonFixture;
import com.daesabu.meongcoach.training.domain.LessonProgress;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainedLesson;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import com.daesabu.meongcoach.training.domain.TrainingSource;
import com.daesabu.meongcoach.training.domain.exception.LessonNotFoundException;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class LessonCompleterTest {

	private static final Long USER_ID = 1L;

	private static final Long OTHER_USER_ID = 2L;

	private static final Long DOGLESS_USER_ID = 3L;

	private static final Long ABSENT_LESSON_ID = 999L;

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	@Autowired
	private LessonCompleter lessonCompleter;

	@Autowired
	private TopicSelector topicSelector;

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
	private TrainedLessonRepository trainedLessonRepository;

	@Autowired
	private CurriculumProgressRepository curriculumProgressRepository;

	@Autowired
	private EntitlementRepository entitlementRepository;

	@Autowired
	private DogRepository dogRepository;

	@Autowired
	private EntityManager entityManager;

	private Long dogId;

	private Long otherUserDogId;

	@BeforeEach
	void setUp() {
		dogId = dogRepository.save(DogFixture.selected(USER_ID)).getId();
		otherUserDogId = dogRepository.save(DogFixture.selected(OTHER_USER_ID)).getId();
	}

	@Test
	void 첫_완료_시_완료_횟수_1을_반환한다() {
		Lesson lesson = saveLesson("앉아");
		flushAndClear();

		int completedCount = lessonCompleter.completeLesson(USER_ID, dogId, lesson.getId());

		assertThat(completedCount).isOne();
	}

	@Test
	void 첫_완료_시_진행도가_생성되고_완료_횟수가_1이_된다() {
		Lesson lesson = saveLesson("앉아");
		flushAndClear();

		lessonCompleter.completeLesson(USER_ID, dogId, lesson.getId());

		flushAndClear();
		assertThat(lessonProgressRepository.count()).isOne();
		assertThat(findCompletedCount(USER_ID, dogId, lesson.getId())).isOne();
	}

	@Test
	void 반복_완료_시_호출할_때마다_완료_횟수가_1씩_증가한다() {
		Lesson lesson = saveLesson("앉아");
		flushAndClear();

		assertThat(lessonCompleter.completeLesson(USER_ID, dogId, lesson.getId())).isEqualTo(1);
		assertThat(lessonCompleter.completeLesson(USER_ID, dogId, lesson.getId())).isEqualTo(2);
		assertThat(lessonCompleter.completeLesson(USER_ID, dogId, lesson.getId())).isEqualTo(3);

		flushAndClear();
		assertThat(lessonProgressRepository.count()).isOne();
		assertThat(findCompletedCount(USER_ID, dogId, lesson.getId())).isEqualTo(3);
	}

	@Test
	void 다른_사용자의_완료_횟수에는_영향을_주지_않는다() {
		Lesson lesson = saveLesson("앉아");
		flushAndClear();
		lessonCompleter.completeLesson(OTHER_USER_ID, otherUserDogId, lesson.getId());
		lessonCompleter.completeLesson(OTHER_USER_ID, otherUserDogId, lesson.getId());

		int completedCount = lessonCompleter.completeLesson(USER_ID, dogId, lesson.getId());

		flushAndClear();
		assertThat(completedCount).isOne();
		assertThat(findCompletedCount(USER_ID, dogId, lesson.getId())).isOne();
		assertThat(findCompletedCount(OTHER_USER_ID, otherUserDogId, lesson.getId())).isEqualTo(2);
	}

	@Test
	void 같은_사용자의_다른_강아지_완료_횟수에는_영향을_주지_않는다() {
		Long secondDogId = dogRepository.save(DogFixture.unselected(USER_ID)).getId();
		Lesson lesson = saveLesson("앉아");
		flushAndClear();
		lessonCompleter.completeLesson(USER_ID, dogId, lesson.getId());
		lessonCompleter.completeLesson(USER_ID, dogId, lesson.getId());

		int completedCount = lessonCompleter.completeLesson(USER_ID, secondDogId, lesson.getId());

		flushAndClear();
		assertThat(completedCount).isOne();
		assertThat(findCompletedCount(USER_ID, secondDogId, lesson.getId())).isOne();
		assertThat(findCompletedCount(USER_ID, dogId, lesson.getId())).isEqualTo(2);
	}

	@Test
	void dogId를_생략하면_선택된_강아지에_기록한다() {
		dogRepository.save(DogFixture.unselected(USER_ID));
		Lesson lesson = saveLesson("앉아");
		flushAndClear();

		int completedCount = lessonCompleter.completeLesson(USER_ID, null, lesson.getId());

		flushAndClear();
		assertThat(completedCount).isOne();
		assertThat(lessonProgressRepository.count()).isOne();
		assertThat(findCompletedCount(USER_ID, dogId, lesson.getId())).isOne();
	}

	@Test
	void 다른_사용자의_강아지를_지정하면_예외를_던지고_진행도를_만들지_않는다() {
		Lesson lesson = saveLesson("앉아");
		flushAndClear();

		assertThatThrownBy(() -> lessonCompleter.completeLesson(USER_ID, otherUserDogId, lesson.getId()))
				.isInstanceOf(DogNotFoundException.class);

		flushAndClear();
		assertThat(lessonProgressRepository.count()).isZero();
		assertThat(trainedLessonRepository.count()).isZero();
		assertThat(curriculumProgressRepository.count()).isZero();
	}

	@Test
	void 선택된_강아지가_없는_사용자가_dogId를_생략하면_예외를_던진다() {
		Lesson lesson = saveLesson("앉아");
		flushAndClear();

		assertThatThrownBy(() -> lessonCompleter.completeLesson(DOGLESS_USER_ID, null, lesson.getId()))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	void 강아지가_없는_사용자도_존재하지_않는_레슨이면_레슨_예외를_던진다() {
		assertThatThrownBy(() -> lessonCompleter.completeLesson(DOGLESS_USER_ID, null, ABSENT_LESSON_ID))
				.isInstanceOf(LessonNotFoundException.class);
	}

	@Test
	void 존재하지_않는_레슨이면_예외를_던진다() {
		assertThatThrownBy(() -> lessonCompleter.completeLesson(USER_ID, dogId, ABSENT_LESSON_ID))
				.isInstanceOf(LessonNotFoundException.class);
	}

	@Test
	void 존재하지_않는_레슨이면_진행도를_변경하지_않는다() {
		Lesson lesson = saveLesson("앉아");
		flushAndClear();
		lessonCompleter.completeLesson(USER_ID, dogId, lesson.getId());
		flushAndClear();

		assertThatThrownBy(() -> lessonCompleter.completeLesson(USER_ID, dogId, ABSENT_LESSON_ID))
				.isInstanceOf(LessonNotFoundException.class);

		flushAndClear();
		assertThat(lessonProgressRepository.count()).isOne();
		assertThat(findCompletedCount(USER_ID, dogId, lesson.getId())).isOne();
	}

	@Test
	void 이용권_없이_유료_레슨을_완료하면_예외를_던지고_진행도를_만들지_않는다() {
		Lesson lesson = savePremiumLesson("앉아", EntitlementType.PUPPY);
		flushAndClear();

		assertThatThrownBy(() -> lessonCompleter.completeLesson(USER_ID, dogId, lesson.getId()))
				.isInstanceOf(EntitlementRequiredException.class);

		flushAndClear();
		assertThat(lessonProgressRepository.count()).isZero();
	}

	@Test
	void 이용권이_있으면_유료_레슨을_완료한다() {
		Lesson lesson = savePremiumLesson("앉아", EntitlementType.PUPPY);
		entitlementRepository.save(EntitlementFixture.create(USER_ID, EntitlementType.PUPPY, null));
		flushAndClear();

		int completedCount = lessonCompleter.completeLesson(USER_ID, dogId, lesson.getId());

		assertThat(completedCount).isOne();
	}

	@Test
	void 추가된_레슨과_커리큘럼의_진행을_독립적으로_기록한다() {
		Lesson first = saveLesson("앉아");
		Curriculum firstCurriculum = first.getCurriculum();
		Topic topic = firstCurriculum.getTopic();
		flushAndClear();
		lessonCompleter.completeLesson(USER_ID, dogId, first.getId());
		flushAndClear();
		Lesson second = lessonRepository.save(LessonFixture.create(firstCurriculum, "기다려", 2, 5));
		Curriculum next = curriculumRepository.save(CurriculumFixture.create(topic, "다음 단계", 2, null, null));
		Lesson third = lessonRepository.save(LessonFixture.create(next, "멀리서 기다려", 1, 5));
		flushAndClear();

		lessonCompleter.completeLesson(USER_ID, dogId, second.getId());
		lessonCompleter.completeLesson(USER_ID, dogId, third.getId());
		flushAndClear();

		assertThat(lessonProgressRepository.count()).isEqualTo(3);
		assertThat(findCompletedCount(USER_ID, dogId, first.getId())).isOne();
		assertThat(findCompletedCount(USER_ID, dogId, second.getId())).isOne();
		assertThat(findCompletedCount(USER_ID, dogId, third.getId())).isOne();
	}

	@Test
	void 선택_토픽을_바꿔도_토픽별_진행을_유지한다() {
		Lesson first = saveLesson("앉아");
		Lesson second = saveLesson("기다려");
		Long secondTopicId = second.getCurriculum().getTopic().getId();
		flushAndClear();
		lessonCompleter.completeLesson(USER_ID, dogId, first.getId());

		topicSelector.selectTopic(USER_ID, dogId, new TopicSelectionRequest(secondTopicId));
		lessonCompleter.completeLesson(USER_ID, dogId, second.getId());
		flushAndClear();

		assertThat(lessonProgressRepository.count()).isEqualTo(2);
		assertThat(findCompletedCount(USER_ID, dogId, first.getId())).isOne();
		assertThat(findCompletedCount(USER_ID, dogId, second.getId())).isOne();
	}

	@Test
	void 레슨을_완료하면_그날_라이브러리에서_훈련한_기록을_남긴다() {
		Lesson lesson = saveLesson("앉아");
		flushAndClear();

		lessonCompleter.completeLesson(USER_ID, dogId, lesson.getId());
		flushAndClear();

		List<TrainedLesson> trainedLessons = findTrainedLessons(USER_ID, dogId);
		assertThat(trainedLessons).hasSize(1);
		TrainedLesson trainedLesson = trainedLessons.getFirst();
		assertThat(trainedLesson.getLessonId()).isEqualTo(lesson.getId());
		assertThat(trainedLesson.getSource()).isEqualTo(TrainingSource.LIBRARY);
		assertThat(trainedLesson.getTrainedDate()).isEqualTo(today());
	}

	@Test
	void 레슨을_완료하면_그_커리큘럼의_그날_진행_기록이_판정을_기다리는_상태로_생긴다() {
		Lesson lesson = saveLesson("앉아");
		Long curriculumId = lesson.getCurriculum().getId();
		flushAndClear();

		lessonCompleter.completeLesson(USER_ID, dogId, lesson.getId());
		flushAndClear();

		assertThat(findCurriculumProgress(USER_ID, dogId, curriculumId))
				.get().extracting(CurriculumProgress::getStatus).isEqualTo(CurriculumProgressStatus.PENDING);
	}

	@Test
	void 같은_날_두_번_완료하면_횟수는_2가_되고_훈련한_기록은_하나만_남는다() {
		Lesson lesson = saveLesson("앉아");
		flushAndClear();

		lessonCompleter.completeLesson(USER_ID, dogId, lesson.getId());
		int completedCount = lessonCompleter.completeLesson(USER_ID, dogId, lesson.getId());
		flushAndClear();

		assertThat(completedCount).isEqualTo(2);
		assertThat(findTrainedLessons(USER_ID, dogId)).hasSize(1);
		assertThat(curriculumProgressRepository.count()).isOne();
	}

	@Test
	void 같은_커리큘럼의_다른_레슨을_완료하면_레슨마다_기록하고_그날_진행_기록은_하나만_둔다() {
		Lesson first = saveLesson("앉아");
		Lesson second = lessonRepository.save(LessonFixture.create(first.getCurriculum(), "기다려", 2, 5));
		flushAndClear();

		lessonCompleter.completeLesson(USER_ID, dogId, first.getId());
		lessonCompleter.completeLesson(USER_ID, dogId, second.getId());
		flushAndClear();

		assertThat(findTrainedLessons(USER_ID, dogId)).extracting(TrainedLesson::getLessonId)
				.containsExactlyInAnyOrder(first.getId(), second.getId());
		assertThat(curriculumProgressRepository.count()).isOne();
	}

	@Test
	void 그날_결과를_고른_커리큘럼의_레슨을_다시_완료해도_결과를_유지한다() {
		Lesson lesson = saveLesson("앉아");
		Long curriculumId = lesson.getCurriculum().getId();
		flushAndClear();
		lessonCompleter.completeLesson(USER_ID, dogId, lesson.getId());
		findCurriculumProgress(USER_ID, dogId, curriculumId).orElseThrow().succeed();
		flushAndClear();

		lessonCompleter.completeLesson(USER_ID, dogId, lesson.getId());
		flushAndClear();

		assertThat(findCurriculumProgress(USER_ID, dogId, curriculumId))
				.get().extracting(CurriculumProgress::getStatus).isEqualTo(CurriculumProgressStatus.SUCCEEDED);
	}

	@Test
	void dogId를_생략하면_선택된_강아지에_훈련한_기록을_남긴다() {
		Long unselectedDogId = dogRepository.save(DogFixture.unselected(USER_ID)).getId();
		Lesson lesson = saveLesson("앉아");
		flushAndClear();

		lessonCompleter.completeLesson(USER_ID, null, lesson.getId());
		flushAndClear();

		assertThat(findTrainedLessons(USER_ID, dogId)).hasSize(1);
		assertThat(findTrainedLessons(USER_ID, unselectedDogId)).isEmpty();
	}

	@Test
	void 같은_사용자의_다른_강아지로_완료하면_강아지마다_따로_기록한다() {
		Long unselectedDogId = dogRepository.save(DogFixture.unselected(USER_ID)).getId();
		Lesson lesson = saveLesson("앉아");
		flushAndClear();

		lessonCompleter.completeLesson(USER_ID, dogId, lesson.getId());
		lessonCompleter.completeLesson(USER_ID, unselectedDogId, lesson.getId());
		flushAndClear();

		assertThat(findTrainedLessons(USER_ID, dogId)).hasSize(1);
		assertThat(findTrainedLessons(USER_ID, unselectedDogId)).hasSize(1);
		assertThat(curriculumProgressRepository.count()).isEqualTo(2);
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

	private int findCompletedCount(Long userId, Long dogId, Long lessonId) {
		return lessonProgressRepository.findByUserIdAndDogIdAndLessonId(userId, dogId, lessonId)
				.map(LessonProgress::getCompletedCount)
				.orElseThrow();
	}

	private List<TrainedLesson> findTrainedLessons(Long userId, Long dogId) {
		return trainedLessonRepository.findAllByUserIdAndDogIdAndTrainedDateBetween(userId, dogId, today(), today());
	}

	private Optional<CurriculumProgress> findCurriculumProgress(Long userId, Long dogId, Long curriculumId) {
		return curriculumProgressRepository.findByUserIdAndDogIdAndCurriculumIdAndTrainedDate(userId, dogId,
				curriculumId, today());
	}

	private LocalDate today() {
		return LocalDate.now(SEOUL);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
