package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.DogFixture;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.provided.dto.CurriculumDetailResult;
import com.daesabu.meongcoach.training.application.provided.dto.CurriculumListResult;
import com.daesabu.meongcoach.training.application.provided.dto.CurriculumResult;
import com.daesabu.meongcoach.training.application.provided.dto.TopicSelectionRequest;
import com.daesabu.meongcoach.training.application.required.CurriculumRepository;
import com.daesabu.meongcoach.training.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.application.required.TopicSelectionRepository;
import com.daesabu.meongcoach.training.application.required.TrainingCategoryRepository;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumFixture;
import com.daesabu.meongcoach.training.domain.Expert;
import com.daesabu.meongcoach.training.domain.ExpertFixture;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonFixture;
import com.daesabu.meongcoach.training.domain.LessonProgress;
import com.daesabu.meongcoach.training.domain.ProgressStatus;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TopicSelection;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import com.daesabu.meongcoach.training.domain.exception.CurriculumNotFoundException;
import com.daesabu.meongcoach.training.domain.exception.TopicNotConfiguredException;
import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.stream.Collectors;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class CurriculumFinderTest {

	private static final Long USER_ID = 42L;

	private static final Long OTHER_USER_ID = 99L;

	@Autowired
	private TopicSelector topicSelector;

	@Autowired
	private LessonProgressRepository lessonProgressRepository;

	@Autowired
	private CurriculumFinder curriculumFinder;

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
	private TopicSelectionRepository topicSelectionRepository;

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
	void 같은_사용자의_다른_강아지가_선택한_토픽을_사용하지_않는다() {
		Long secondDogId = dogRepository.save(DogFixture.unselected(USER_ID)).getId();
		TrainingCategory category = saveCategory("기본 교육", 1);
		Topic first = saveTopic(category, "앉아", 1);
		Topic second = saveTopic(category, "기다려", 2);
		topicSelectionRepository.save(TopicSelection.enter(USER_ID, secondDogId, second.getId()));
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID, dogId);

		assertThat(curriculumList.topic().getId()).isEqualTo(first.getId());
	}

	@Test
	void 커리큘럼_목록에서_같은_사용자의_다른_강아지_완료_기록은_제외한다() {
		Long secondDogId = dogRepository.save(DogFixture.unselected(USER_ID)).getId();
		Topic topic = saveTopicWithCategory();
		Curriculum curriculum = saveCurriculum(topic, "단계", 1);
		Lesson lesson = saveLesson(curriculum, "레슨", 1);
		complete(USER_ID, secondDogId, lesson, 1);
		flushAndClear();

		Map<Long, Tuple> completions = findCompletions(USER_ID, dogId, topic.getId());

		assertThat(completions).containsOnly(
				entry(curriculum.getId(), tuple(0, ProgressStatus.NOT_STARTED)));
	}

	@Test
	void 커리큘럼_상세에서_같은_사용자의_다른_강아지_완료_기록은_세지_않는다() {
		Long secondDogId = dogRepository.save(DogFixture.unselected(USER_ID)).getId();
		Topic topic = saveTopicWithCategory();
		Curriculum curriculum = saveCurriculum(topic, "단계", 1);
		Lesson lesson = saveLesson(curriculum, "레슨", 1);
		complete(USER_ID, secondDogId, lesson, 2);
		flushAndClear();

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, dogId, curriculum.getId());

		assertThat(detail.progresses().completedCount(lesson.getId())).isZero();
	}

	@Test
	void dogId를_생략하면_선택된_강아지의_선택_토픽과_진행으로_조회한다() {
		Long secondDogId = dogRepository.save(DogFixture.unselected(USER_ID)).getId();
		TrainingCategory category = saveCategory("기본 교육", 1);
		saveTopic(category, "앉아", 1);
		Topic second = saveTopic(category, "기다려", 2);
		Curriculum curriculum = saveCurriculum(second, "단계", 1);
		Lesson lesson = saveLesson(curriculum, "레슨", 1);
		topicSelectionRepository.save(TopicSelection.enter(USER_ID, dogId, second.getId()));
		complete(USER_ID, dogId, lesson, 1);
		complete(USER_ID, secondDogId, lesson, 3);
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID, null);
		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, null, curriculum.getId());

		assertThat(curriculumList.topic().getId()).isEqualTo(second.getId());
		assertThat(curriculumList.curriculums().getFirst().completedLessons()).isOne();
		assertThat(detail.progresses().completedCount(lesson.getId())).isOne();
	}

	@Test
	void 다른_사용자의_강아지를_지정하면_예외를_던진다() {
		Topic topic = saveTopicWithCategory();
		Curriculum curriculum = saveCurriculum(topic, "단계", 1);
		flushAndClear();

		assertThatThrownBy(() -> curriculumFinder.findCurriculums(USER_ID, otherUserDogId))
				.isInstanceOf(DogNotFoundException.class);
		assertThatThrownBy(() -> curriculumFinder.findCurriculum(USER_ID, otherUserDogId, curriculum.getId()))
				.isInstanceOf(DogNotFoundException.class);
	}

	@Test
	void 가장_최근에_진입한_토픽의_커리큘럼을_반환한다() {
		TrainingCategory category = saveCategory("기본 교육", 1);
		Topic first = saveTopic(category, "앉아", 1);
		Topic second = saveTopic(category, "기다려", 2);
		Curriculum firstCurriculum = saveCurriculum(first, "앉아 1단계", 1);
		saveLesson(firstCurriculum, "레슨", 1);
		Curriculum secondCurriculum = saveCurriculum(second, "기다려 1단계", 1);
		saveLesson(secondCurriculum, "레슨", 1);
		flushAndClear();
		topicSelectionRepository.save(TopicSelection.enter(USER_ID, dogId, second.getId()));
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID, dogId);

		assertThat(curriculumList.topic().getId()).isEqualTo(second.getId());
		assertThat(curriculumList.curriculums()).extracting(result -> result.curriculum().getTitle())
				.containsExactly("기다려 1단계");
	}

	@Test
	void 진입_기록이_없으면_첫_토픽으로_폴백한다() {
		TrainingCategory advanced = saveCategory("심화 교육", 2);
		TrainingCategory basic = saveCategory("기본 교육", 1);
		Topic advancedTopic = saveTopic(advanced, "이리와", 1);
		Topic basicTopic = saveTopic(basic, "앉아", 1);
		Curriculum advancedCurriculum = saveCurriculum(advancedTopic, "이리와 1단계", 1);
		saveLesson(advancedCurriculum, "레슨", 1);
		Curriculum basicCurriculum = saveCurriculum(basicTopic, "앉아 1단계", 1);
		saveLesson(basicCurriculum, "레슨", 1);
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID, dogId);

		assertThat(curriculumList.topic().getId()).isEqualTo(basicTopic.getId());
		assertThat(curriculumList.curriculums()).extracting(result -> result.curriculum().getTitle())
				.containsExactly("앉아 1단계");
	}

	@Test
	void 진입_기록의_토픽이_더_이상_존재하지_않으면_첫_토픽으로_폴백한다() {
		TrainingCategory category = saveCategory("기본 교육", 1);
		Topic topic = saveTopic(category, "앉아", 1);
		Curriculum curriculum = saveCurriculum(topic, "앉아 1단계", 1);
		saveLesson(curriculum, "레슨", 1);
		flushAndClear();
		topicSelectionRepository.save(TopicSelection.enter(USER_ID, dogId, 999L));
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID, dogId);

		assertThat(curriculumList.topic().getId()).isEqualTo(topic.getId());
	}

	@Test
	void 등록된_토픽이_하나도_없으면_예외를_던진다() {
		assertThatThrownBy(() -> curriculumFinder.findCurriculums(USER_ID, dogId))
				.isInstanceOf(TopicNotConfiguredException.class);
	}

	@Test
	void 다른_사용자의_선택_토픽을_사용하지_않는다() {
		TrainingCategory category = saveCategory("기본 교육", 1);
		Topic first = saveTopic(category, "앉아", 1);
		Topic second = saveTopic(category, "기다려", 2);
		topicSelectionRepository.save(TopicSelection.enter(OTHER_USER_ID, otherUserDogId, second.getId()));
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID, dogId);

		assertThat(curriculumList.topic().getId()).isEqualTo(first.getId());
	}

	@Test
	void 커리큘럼을_정렬_순서_오름차순으로_반환한다() {
		Topic topic = saveTopicWithCategory();
		Curriculum third = saveCurriculum(topic, "셋째 커리큘럼", 3);
		saveLesson(third, "레슨", 1);
		Curriculum first = saveCurriculum(topic, "첫째 커리큘럼", 1);
		saveLesson(first, "레슨", 1);
		Curriculum second = saveCurriculum(topic, "둘째 커리큘럼", 2);
		saveLesson(second, "레슨", 1);
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID, dogId);

		assertThat(curriculumList.curriculums()).extracting(result -> result.curriculum().getTitle())
				.containsExactly("첫째 커리큘럼", "둘째 커리큘럼", "셋째 커리큘럼");
	}

	@Test
	void 커리큘럼별_완료_개수와_진행_상태를_반환한다() {
		Topic topic = saveTopicWithCategory();
		Curriculum first = saveCurriculum(topic, "1단계", 1);
		Curriculum second = saveCurriculum(topic, "2단계", 2);
		Lesson completed = saveLesson(first, "손 위의 간식", 1);
		saveLesson(first, "간식 없이 앉아", 2);
		saveLesson(second, "거리 두고 앉아", 1);
		flushAndClear();
		lessonCompleter.completeLesson(USER_ID, dogId, completed.getId());
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID, dogId);

		assertThat(curriculumList.curriculums()).extracting(CurriculumResult::completedLessons, CurriculumResult::status)
				.containsExactly(tuple(1, ProgressStatus.IN_PROGRESS),
						tuple(0, ProgressStatus.NOT_STARTED));
	}

	@Test
	void 커리큘럼_목록에서_다른_사용자의_완료_기록은_제외한다() {
		Topic topic = saveTopicWithCategory();
		Curriculum curriculum = saveCurriculum(topic, "1단계", 1);
		Lesson lesson = saveLesson(curriculum, "손 위의 간식", 1);
		flushAndClear();
		lessonCompleter.completeLesson(OTHER_USER_ID, otherUserDogId, lesson.getId());
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID, dogId);

		assertThat(curriculumList.curriculums()).extracting(CurriculumResult::completedLessons, CurriculumResult::status)
				.containsExactly(tuple(0, ProgressStatus.NOT_STARTED));
	}

	@Test
	void 레슨이_없는_커리큘럼의_진행_상태를_조회하면_예외를_던진다() {
		Topic topic = saveTopicWithCategory();
		saveCurriculum(topic, "레슨 없는 커리큘럼", 1);
		Curriculum other = saveCurriculum(topic, "레슨 있는 커리큘럼", 2);
		saveLesson(other, "손 위의 간식", 1);
		flushAndClear();

		assertThatThrownBy(() -> curriculumFinder.findCurriculums(USER_ID, dogId))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 조회만_하고_진입_기록을_저장하지_않는다() {
		Topic topic = saveTopicWithCategory();
		Curriculum curriculum = saveCurriculum(topic, "1단계", 1);
		saveLesson(curriculum, "레슨", 1);
		flushAndClear();

		curriculumFinder.findCurriculums(USER_ID, dogId);
		flushAndClear();

		assertThat(topicSelectionRepository.count()).isZero();
	}

	@Test
	void 커리큘럼과_그_레슨_목록을_반환한다() {
		Topic topic = saveTopicWithCategory();
		Curriculum curriculum = saveCurriculum(topic, "앉아 2단계", 2);
		saveLesson(curriculum, "손 위의 간식", 1);
		saveLesson(curriculum, "간식 없이 앉아", 2);
		flushAndClear();

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, dogId, curriculum.getId());

		assertThat(detail.curriculum().getId()).isEqualTo(curriculum.getId());
		assertThat(detail.curriculum().getLessons()).extracting(Lesson::getTitle)
				.containsExactly("손 위의 간식", "간식 없이 앉아");
	}

	@Test
	void 커리큘럼_상세에_제작_전문가를_포함한다() {
		Topic topic = saveTopicWithCategory();
		Expert expert = ExpertFixture.create("정은겸", "교수", null, null);
		entityManager.persist(expert);
		Curriculum curriculum = curriculumRepository.save(
				CurriculumFixture.create(topic, "앉아 1단계", 1, null, null, expert));
		flushAndClear();

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, dogId, curriculum.getId());

		assertThat(detail.curriculum().findExpert()).map(Expert::getName).contains("정은겸");
	}

	@Test
	void 제작_전문가가_없는_커리큘럼_상세는_전문가를_비운다() {
		Topic topic = saveTopicWithCategory();
		Curriculum curriculum = saveCurriculum(topic, "앉아 1단계", 1);
		flushAndClear();

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, dogId, curriculum.getId());

		assertThat(detail.curriculum().findExpert()).isEmpty();
	}

	@Test
	void 레슨을_정렬_순서_오름차순으로_반환하고_다른_커리큘럼의_레슨은_제외한다() {
		Topic topic = saveTopicWithCategory();
		Curriculum curriculum = saveCurriculum(topic, "1단계", 1);
		Curriculum other = saveCurriculum(topic, "2단계", 2);
		saveLesson(curriculum, "셋째 레슨", 3);
		saveLesson(curriculum, "첫째 레슨", 1);
		saveLesson(curriculum, "둘째 레슨", 2);
		saveLesson(other, "다른 커리큘럼 레슨", 1);
		flushAndClear();

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, dogId, curriculum.getId());

		assertThat(detail.curriculum().getLessons()).extracting(Lesson::getTitle)
				.containsExactly("첫째 레슨", "둘째 레슨", "셋째 레슨");
	}

	@Test
	void 레슨마다_사용자의_반복_완료_횟수를_반환하고_기록이_없으면_0으로_채운다() {
		Topic topic = saveTopicWithCategory();
		Curriculum curriculum = saveCurriculum(topic, "1단계", 1);
		Lesson twice = saveLesson(curriculum, "첫째 레슨", 1);
		Lesson once = saveLesson(curriculum, "둘째 레슨", 2);
		Lesson none = saveLesson(curriculum, "셋째 레슨", 3);
		flushAndClear();
		lessonCompleter.completeLesson(USER_ID, dogId, twice.getId());
		lessonCompleter.completeLesson(USER_ID, dogId, twice.getId());
		lessonCompleter.completeLesson(USER_ID, dogId, once.getId());
		flushAndClear();

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, dogId, curriculum.getId());

		assertThat(detail.progresses().completedCount(twice.getId())).isEqualTo(2);
		assertThat(detail.progresses().completedCount(once.getId())).isEqualTo(1);
		assertThat(detail.progresses().completedCount(none.getId())).isZero();
	}

	@Test
	void 커리큘럼_상세에서_다른_사용자의_완료_기록은_세지_않는다() {
		Topic topic = saveTopicWithCategory();
		Curriculum curriculum = saveCurriculum(topic, "1단계", 1);
		Lesson lesson = saveLesson(curriculum, "손 위의 간식", 1);
		flushAndClear();
		lessonCompleter.completeLesson(OTHER_USER_ID, otherUserDogId, lesson.getId());
		flushAndClear();

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, dogId, curriculum.getId());

		assertThat(detail.progresses().completedCount(lesson.getId())).isZero();
	}

	@Test
	void 존재하지_않는_커리큘럼이면_예외를_던진다() {
		assertThatThrownBy(() -> curriculumFinder.findCurriculum(USER_ID, dogId, 999L))
				.isInstanceOf(CurriculumNotFoundException.class);
	}

	@Test
	void 레슨이_없는_커리큘럼은_빈_레슨_목록을_반환한다() {
		Topic topic = saveTopicWithCategory();
		Curriculum empty = saveCurriculum(topic, "레슨 없는 커리큘럼", 1);
		Curriculum other = saveCurriculum(topic, "레슨 있는 커리큘럼", 2);
		saveLesson(other, "손 위의 간식", 1);
		flushAndClear();

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, dogId, empty.getId());

		assertThat(detail.curriculum().getLessons()).isEmpty();
	}

	@Test
	void 진행_기록이_없어도_모든_커리큘럼을_시작_전으로_조회한다() {
		Topic topic = saveTopicWithCategory();
		Curriculum curriculum = saveCurriculum(topic, "단계", 1);
		saveLesson(curriculum, "레슨", 1);
		Curriculum second = saveCurriculum(topic, "단계", 2);
		saveLesson(second, "레슨", 1);
		flushAndClear();

		Map<Long, Tuple> completions = findCompletions(USER_ID, dogId, topic.getId());

		assertThat(completions).containsOnly(
				entry(curriculum.getId(), tuple(0, ProgressStatus.NOT_STARTED)),
				entry(second.getId(), tuple(0, ProgressStatus.NOT_STARTED)));
		assertThat(lessonProgressRepository.count()).isZero();
	}

	@Test
	void 선택한_토픽의_커리큘럼별_완료_레슨_수와_상태를_조회한다() {
		Topic topic = saveTopicWithCategory();
		Curriculum first = saveCurriculum(topic, "단계", 1);
		Lesson firstLesson = saveLesson(first, "레슨", 1);
		saveLesson(first, "레슨", 2);
		Curriculum second = saveCurriculum(topic, "단계", 2);
		Lesson secondLesson = saveLesson(second, "레슨", 1);
		Curriculum third = saveCurriculum(topic, "단계", 3);
		saveLesson(third, "레슨", 1);
		complete(USER_ID, dogId, firstLesson, 2);
		complete(USER_ID, dogId, secondLesson, 1);
		flushAndClear();

		Map<Long, Tuple> completions = findCompletions(USER_ID, dogId, topic.getId());

		assertThat(completions).containsOnly(
				entry(first.getId(), tuple(1, ProgressStatus.IN_PROGRESS)),
				entry(second.getId(), tuple(1, ProgressStatus.COMPLETED)),
				entry(third.getId(), tuple(0, ProgressStatus.NOT_STARTED)));
	}

	@Test
	void 다른_사용자와_다른_토픽의_완료_기록은_제외한다() {
		Topic topic = saveTopicWithCategory();
		Curriculum curriculum = saveCurriculum(topic, "단계", 1);
		Lesson lesson = saveLesson(curriculum, "레슨", 1);
		complete(43L, 430L, lesson, 1);
		Topic otherTopic = saveTopicWithCategory();
		Curriculum otherCurriculum = saveCurriculum(otherTopic, "단계", 1);
		Lesson otherLesson = saveLesson(otherCurriculum, "레슨", 1);
		complete(USER_ID, dogId, otherLesson, 1);
		flushAndClear();

		Map<Long, Tuple> completions = findCompletions(USER_ID, dogId, topic.getId());

		assertThat(completions).containsOnly(
				entry(curriculum.getId(), tuple(0, ProgressStatus.NOT_STARTED)));
	}

	@Test
	void 완료한_커리큘럼에_새_레슨이_추가되면_진행_중으로_조회한다() {
		Topic topic = saveTopicWithCategory();
		Curriculum curriculum = saveCurriculum(topic, "단계", 1);
		Lesson lesson = saveLesson(curriculum, "레슨", 1);
		complete(USER_ID, dogId, lesson, 1);
		flushAndClear();
		assertThat(findCompletions(USER_ID, dogId, topic.getId()))
				.containsEntry(curriculum.getId(), tuple(1, ProgressStatus.COMPLETED));
		saveLesson(curriculum, "레슨", 2);
		flushAndClear();

		Map<Long, Tuple> completions = findCompletions(USER_ID, dogId, topic.getId());

		assertThat(completions)
				.containsEntry(curriculum.getId(), tuple(1, ProgressStatus.IN_PROGRESS));
	}

	@Test
	void 현재_콘텐츠에서_삭제된_레슨의_완료_기록은_세지_않는다() {
		Topic topic = saveTopicWithCategory();
		Curriculum curriculum = saveCurriculum(topic, "단계", 1);
		Lesson removed = saveLesson(curriculum, "레슨", 1);
		saveLesson(curriculum, "레슨", 2);
		complete(USER_ID, dogId, removed, 1);
		lessonRepository.delete(removed);
		flushAndClear();

		Map<Long, Tuple> completions = findCompletions(USER_ID, dogId, topic.getId());

		assertThat(completions)
				.containsEntry(curriculum.getId(), tuple(0, ProgressStatus.NOT_STARTED));
	}

	@Test
	void 커리큘럼이_없는_토픽은_빈_완료_정보를_반환한다() {
		Topic topic = saveTopicWithCategory();
		flushAndClear();

		assertThat(findCompletions(USER_ID, dogId, topic.getId())).isEmpty();
	}

	private TrainingCategory saveCategory(String title, int sortOrder) {
		return trainingCategoryRepository.save(TrainingCategoryFixture.create(title, sortOrder, null, null));
	}

	private Topic saveTopic(TrainingCategory category, String title, int sortOrder) {
		return topicRepository.save(TopicFixture.create(category, title, sortOrder, null, null, null));
	}

	private Topic saveTopicWithCategory() {
		return saveTopic(saveCategory("기본 교육", 1), "앉아", 1);
	}

	private Curriculum saveCurriculum(Topic topic, String title, int sortOrder) {
		return curriculumRepository.save(CurriculumFixture.create(topic, title, sortOrder, null, null));
	}

	private Lesson saveLesson(Curriculum curriculum, String title, int sortOrder) {
		return lessonRepository.save(LessonFixture.create(curriculum, title, sortOrder, 5));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	private void complete(Long userId, Long dogId, Lesson lesson, int count) {
		LessonProgress progress = LessonProgress.start(userId, dogId, lesson.getId());
		for (int i = 0; i < count; i++) {
			progress.complete();
		}
		lessonProgressRepository.save(progress);
	}

	private Map<Long, Tuple> findCompletions(Long userId, Long dogId, Long topicId) {
		topicSelector.selectTopic(userId, dogId, new TopicSelectionRequest(topicId));
		CurriculumListResult result = curriculumFinder.findCurriculums(userId, dogId);
		return result.curriculums().stream().collect(Collectors.toMap(
				item -> item.curriculum().getId(), item -> tuple(item.completedLessons(), item.status())));
	}
}
