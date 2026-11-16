package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.DogFixture;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarCurriculumResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarDayResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarLessonResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarTopicResult;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumFixture;
import com.daesabu.meongcoach.training.domain.CurriculumProgress;
import com.daesabu.meongcoach.training.domain.CurriculumProgressRegisterCommand;
import com.daesabu.meongcoach.training.domain.CurriculumProgressStatus;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonFixture;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainedLesson;
import com.daesabu.meongcoach.training.domain.TrainedLessonRecordCommand;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import com.daesabu.meongcoach.training.domain.TrainingSource;
import com.daesabu.meongcoach.training.domain.exception.InvalidTrainingCalendarPeriodException;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class TrainingCalendarFinderTest {

	private static final Long USER_ID = 1L;

	private static final Long OTHER_USER_ID = 2L;

	private static final LocalDate FROM = LocalDate.of(2026, 11, 1);

	private static final LocalDate TO = LocalDate.of(2026, 11, 30);

	private static final LocalDate DAY = LocalDate.of(2026, 11, 16);

	@Autowired
	private TrainingCalendarFinder trainingCalendarFinder;

	@Autowired
	private DogRepository dogRepository;

	@Autowired
	private EntityManager entityManager;

	private Long dogId;

	@BeforeEach
	void setUp() {
		dogId = dogRepository.save(DogFixture.selected(USER_ID)).getId();
	}

	@Test
	void 훈련한_레슨을_날짜와_출처와_토픽과_커리큘럼별로_묶어_반환한다() {
		Topic topic = saveTopic(saveCategory(1), "산책", 1);
		Curriculum curriculum = saveCurriculum(topic, "집안 산책하기", 1);
		Lesson planLesson = saveLesson(curriculum, "보호자 따라오게 하기", 1);
		Lesson libraryLesson = saveLesson(curriculum, "강아지 따라다녀주기", 2);
		saveTrainedLesson(dogId, planLesson, DAY, TrainingSource.PLAN);
		saveTrainedLesson(dogId, libraryLesson, DAY, TrainingSource.LIBRARY);
		flushAndClear();

		TrainingCalendarResult result = trainingCalendarFinder.find(USER_ID, dogId, FROM, TO);

		assertThat(result.days()).hasSize(1);
		TrainingCalendarDayResult day = result.days().getFirst();
		assertThat(day.trainedDate()).isEqualTo(DAY);
		TrainingCalendarTopicResult planTopic = day.plan().getFirst();
		assertThat(planTopic.topic().getTitle()).isEqualTo("산책");
		TrainingCalendarCurriculumResult planCurriculum = planTopic.curriculums().getFirst();
		assertThat(planCurriculum.curriculum().getTitle()).isEqualTo("집안 산책하기");
		assertThat(planCurriculum.lessons()).extracting(lesson -> lesson.lesson().getId())
				.containsExactly(planLesson.getId());
		assertThat(day.library().getFirst().curriculums().getFirst().lessons())
				.extracting(lesson -> lesson.lesson().getId())
				.containsExactly(libraryLesson.getId());
	}

	@Test
	void 훈련한_날만_오름차순으로_반환하고_출처에_기록이_없으면_빈_목록이다() {
		Lesson lesson = saveLesson(saveCurriculum(saveTopic(saveCategory(1), "산책", 1), "커리큘럼", 1), "레슨", 1);
		saveTrainedLesson(dogId, lesson, DAY.plusDays(2), TrainingSource.PLAN);
		saveTrainedLesson(dogId, lesson, DAY, TrainingSource.PLAN);
		flushAndClear();

		TrainingCalendarResult result = trainingCalendarFinder.find(USER_ID, dogId, FROM, TO);

		assertThat(result.days()).extracting(TrainingCalendarDayResult::trainedDate)
				.containsExactly(DAY, DAY.plusDays(2));
		assertThat(result.days().getFirst().library()).isEmpty();
	}

	@Test
	void 조회_기간_밖의_기록은_날짜_목록에_넣지_않는다() {
		Lesson lesson = saveLesson(saveCurriculum(saveTopic(saveCategory(1), "산책", 1), "커리큘럼", 1), "레슨", 1);
		saveTrainedLesson(dogId, lesson, FROM.minusDays(1), TrainingSource.PLAN);
		saveTrainedLesson(dogId, lesson, FROM, TrainingSource.PLAN);
		saveTrainedLesson(dogId, lesson, TO, TrainingSource.PLAN);
		saveTrainedLesson(dogId, lesson, TO.plusDays(1), TrainingSource.PLAN);
		flushAndClear();

		TrainingCalendarResult result = trainingCalendarFinder.find(USER_ID, dogId, FROM, TO);

		assertThat(result.days()).extracting(TrainingCalendarDayResult::trainedDate).containsExactly(FROM, TO);
	}

	@Test
	void 토픽과_커리큘럼과_레슨을_교육_순서로_반환한다() {
		TrainingCategory firstCategory = saveCategory(1);
		TrainingCategory secondCategory = saveCategory(2);
		Topic laterTopic = saveTopic(secondCategory, "뒤 카테고리 토픽", 1);
		Topic secondTopic = saveTopic(firstCategory, "둘째 토픽", 2);
		Topic firstTopic = saveTopic(firstCategory, "첫째 토픽", 1);
		Curriculum secondCurriculum = saveCurriculum(firstTopic, "둘째 커리큘럼", 2);
		Curriculum firstCurriculum = saveCurriculum(firstTopic, "첫째 커리큘럼", 1);
		Lesson secondLesson = saveLesson(firstCurriculum, "둘째 레슨", 2);
		Lesson firstLesson = saveLesson(firstCurriculum, "첫째 레슨", 1);
		Lesson thirdLesson = saveLesson(secondCurriculum, "셋째 레슨", 1);
		Lesson fourthLesson = saveLesson(saveCurriculum(secondTopic, "커리큘럼", 1), "넷째 레슨", 1);
		Lesson fifthLesson = saveLesson(saveCurriculum(laterTopic, "커리큘럼", 1), "다섯째 레슨", 1);
		List.of(fifthLesson, thirdLesson, secondLesson, fourthLesson, firstLesson)
				.forEach(lesson -> saveTrainedLesson(dogId, lesson, DAY, TrainingSource.LIBRARY));
		flushAndClear();

		TrainingCalendarResult result = trainingCalendarFinder.find(USER_ID, dogId, FROM, TO);

		List<TrainingCalendarTopicResult> topics = result.days().getFirst().library();
		assertThat(topics).extracting(topic -> topic.topic().getTitle())
				.containsExactly("첫째 토픽", "둘째 토픽", "뒤 카테고리 토픽");
		assertThat(topics.getFirst().curriculums()).extracting(curriculum -> curriculum.curriculum().getTitle())
				.containsExactly("첫째 커리큘럼", "둘째 커리큘럼");
		assertThat(topics.getFirst().curriculums().getFirst().lessons())
				.extracting(lesson -> lesson.lesson().getTitle())
				.containsExactly("첫째 레슨", "둘째 레슨");
	}

	@Test
	void 레슨의_횟수는_조회_기간_이전을_포함해_그날까지_훈련한_날_수다() {
		Lesson lesson = saveLesson(saveCurriculum(saveTopic(saveCategory(1), "산책", 1), "커리큘럼", 1), "레슨", 1);
		saveTrainedLesson(dogId, lesson, FROM.minusDays(10), TrainingSource.PLAN);
		saveTrainedLesson(dogId, lesson, DAY, TrainingSource.PLAN);
		saveTrainedLesson(dogId, lesson, DAY, TrainingSource.LIBRARY);
		saveTrainedLesson(dogId, lesson, DAY.plusDays(1), TrainingSource.LIBRARY);
		flushAndClear();

		TrainingCalendarResult result = trainingCalendarFinder.find(USER_ID, dogId, FROM, TO);

		assertThat(trainedDays(result.days().getFirst().plan())).isEqualTo(2);
		assertThat(trainedDays(result.days().getFirst().library())).isEqualTo(2);
		assertThat(trainedDays(result.days().getLast().library())).isEqualTo(3);
	}

	@Test
	void 커리큘럼에는_그날의_진행_상태를_담고_같은_날_플랜과_라이브러리의_상태가_같다() {
		Curriculum curriculum = saveCurriculum(saveTopic(saveCategory(1), "산책", 1), "커리큘럼", 1);
		Lesson planLesson = saveLesson(curriculum, "플랜 레슨", 1);
		Lesson libraryLesson = saveLesson(curriculum, "라이브러리 레슨", 2);
		saveTrainedLesson(dogId, planLesson, DAY, TrainingSource.PLAN);
		saveTrainedLesson(dogId, libraryLesson, DAY, TrainingSource.LIBRARY);
		saveTrainedLesson(dogId, planLesson, DAY.plusDays(1), TrainingSource.PLAN);
		saveCurriculumProgress(dogId, curriculum, DAY).succeed();
		saveCurriculumProgress(dogId, curriculum, DAY.plusDays(1));
		flushAndClear();

		TrainingCalendarResult result = trainingCalendarFinder.find(USER_ID, dogId, FROM, TO);

		TrainingCalendarDayResult day = result.days().getFirst();
		assertThat(day.plan().getFirst().curriculums().getFirst().status())
				.isEqualTo(CurriculumProgressStatus.SUCCEEDED);
		assertThat(day.library().getFirst().curriculums().getFirst().status())
				.isEqualTo(CurriculumProgressStatus.SUCCEEDED);
		assertThat(result.days().getLast().plan().getFirst().curriculums().getFirst().status())
				.isEqualTo(CurriculumProgressStatus.PENDING);
	}

	@Test
	void 처음_훈련한_날은_조회_기간과_무관하게_가장_이른_기록의_날짜다() {
		Lesson lesson = saveLesson(saveCurriculum(saveTopic(saveCategory(1), "산책", 1), "커리큘럼", 1), "레슨", 1);
		saveTrainedLesson(dogId, lesson, FROM.minusDays(30), TrainingSource.LIBRARY);
		saveTrainedLesson(dogId, lesson, DAY, TrainingSource.PLAN);
		flushAndClear();

		TrainingCalendarResult result = trainingCalendarFinder.find(USER_ID, dogId, FROM, TO);

		assertThat(result.firstTrainedDate()).isEqualTo(FROM.minusDays(30));
	}

	@Test
	void 기간_안에_기록이_없어도_처음_훈련한_날은_반환한다() {
		Lesson lesson = saveLesson(saveCurriculum(saveTopic(saveCategory(1), "산책", 1), "커리큘럼", 1), "레슨", 1);
		saveTrainedLesson(dogId, lesson, FROM.minusDays(30), TrainingSource.LIBRARY);
		flushAndClear();

		TrainingCalendarResult result = trainingCalendarFinder.find(USER_ID, dogId, FROM, TO);

		assertThat(result.firstTrainedDate()).isEqualTo(FROM.minusDays(30));
		assertThat(result.days()).isEmpty();
	}

	@Test
	void 훈련한_적이_없으면_처음_훈련한_날이_없고_날짜_목록이_비어_있다() {
		TrainingCalendarResult result = trainingCalendarFinder.find(USER_ID, dogId, FROM, TO);

		assertThat(result.firstTrainedDate()).isNull();
		assertThat(result.days()).isEmpty();
	}

	@Test
	void 같은_사용자의_다른_강아지가_훈련한_기록은_섞이지_않는다() {
		Long otherDogId = dogRepository.save(DogFixture.unselected(USER_ID)).getId();
		Curriculum curriculum = saveCurriculum(saveTopic(saveCategory(1), "산책", 1), "커리큘럼", 1);
		Lesson lesson = saveLesson(curriculum, "레슨", 1);
		saveTrainedLesson(otherDogId, lesson, DAY.minusDays(1), TrainingSource.PLAN);
		saveTrainedLesson(otherDogId, lesson, DAY, TrainingSource.PLAN);
		saveCurriculumProgress(otherDogId, curriculum, DAY).succeed();
		saveTrainedLesson(dogId, lesson, DAY, TrainingSource.PLAN);
		flushAndClear();

		TrainingCalendarResult result = trainingCalendarFinder.find(USER_ID, dogId, FROM, TO);

		assertThat(result.firstTrainedDate()).isEqualTo(DAY);
		assertThat(result.days()).hasSize(1);
		assertThat(trainedDays(result.days().getFirst().plan())).isOne();
		assertThat(result.days().getFirst().plan().getFirst().curriculums().getFirst().status())
				.isEqualTo(CurriculumProgressStatus.PENDING);
	}

	@Test
	void 삭제된_레슨의_기록은_빠지고_그날_남은_레슨이_없으면_날짜도_빠진다() {
		Curriculum curriculum = saveCurriculum(saveTopic(saveCategory(1), "산책", 1), "커리큘럼", 1);
		Lesson lesson = saveLesson(curriculum, "남은 레슨", 1);
		saveTrainedLesson(dogId, lesson, DAY, TrainingSource.PLAN);
		saveTrainedLesson(dogId, 999_999L, DAY, TrainingSource.PLAN);
		saveTrainedLesson(dogId, 999_999L, DAY.plusDays(1), TrainingSource.PLAN);
		flushAndClear();

		TrainingCalendarResult result = trainingCalendarFinder.find(USER_ID, dogId, FROM, TO);

		assertThat(result.days()).extracting(TrainingCalendarDayResult::trainedDate).containsExactly(DAY);
		assertThat(result.days().getFirst().plan().getFirst().curriculums().getFirst().lessons()).hasSize(1);
	}

	@Test
	void 다른_사용자의_강아지로_조회하면_예외를_던진다() {
		Long otherUserDogId = dogRepository.save(DogFixture.selected(OTHER_USER_ID)).getId();

		assertThatThrownBy(() -> trainingCalendarFinder.find(USER_ID, otherUserDogId, FROM, TO))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	void 조회_기간이_62일을_넘으면_예외를_던진다() {
		assertThatThrownBy(() -> trainingCalendarFinder.find(USER_ID, dogId, FROM, FROM.plusDays(62)))
				.isInstanceOf(InvalidTrainingCalendarPeriodException.class);
	}

	@Test
	void 시작일이_종료일보다_늦으면_예외를_던진다() {
		assertThatThrownBy(() -> trainingCalendarFinder.find(USER_ID, dogId, TO, FROM))
				.isInstanceOf(InvalidTrainingCalendarPeriodException.class);
	}

	private int trainedDays(List<TrainingCalendarTopicResult> topics) {
		TrainingCalendarLessonResult lesson = topics.getFirst().curriculums().getFirst().lessons().getFirst();
		return lesson.trainedDays();
	}

	private TrainingCategory saveCategory(int sortOrder) {
		TrainingCategory category = TrainingCategoryFixture.create("카테고리", sortOrder, null, null);
		entityManager.persist(category);
		return category;
	}

	private Topic saveTopic(TrainingCategory category, String title, int sortOrder) {
		Topic topic = TopicFixture.create(category, title, sortOrder, null, null, "footprints");
		entityManager.persist(topic);
		return topic;
	}

	private Curriculum saveCurriculum(Topic topic, String title, int sortOrder) {
		Curriculum curriculum = CurriculumFixture.create(topic, title, sortOrder, null, null);
		entityManager.persist(curriculum);
		return curriculum;
	}

	private Lesson saveLesson(Curriculum curriculum, String title, int sortOrder) {
		Lesson lesson = LessonFixture.create(curriculum, title, sortOrder, 5);
		entityManager.persist(lesson);
		return lesson;
	}

	private void saveTrainedLesson(Long dogId, Lesson lesson, LocalDate trainedDate, TrainingSource source) {
		saveTrainedLesson(dogId, lesson.getId(), trainedDate, source);
	}

	private void saveTrainedLesson(Long dogId, Long lessonId, LocalDate trainedDate, TrainingSource source) {
		Instant completedAt = Instant.parse(trainedDate + "T03:00:00Z");
		TrainedLessonRecordCommand command = new TrainedLessonRecordCommand(dogId, lessonId, source, completedAt);
		entityManager.persist(TrainedLesson.record(USER_ID, command));
	}

	private CurriculumProgress saveCurriculumProgress(Long dogId, Curriculum curriculum, LocalDate trainedDate) {
		CurriculumProgressRegisterCommand command = new CurriculumProgressRegisterCommand(dogId, curriculum.getId(),
				trainedDate);
		CurriculumProgress curriculumProgress = CurriculumProgress.register(USER_ID, command);
		entityManager.persist(curriculumProgress);
		return curriculumProgress;
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
