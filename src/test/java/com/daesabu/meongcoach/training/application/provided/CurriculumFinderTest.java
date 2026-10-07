package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.EntitlementFixture;
import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementRequiredException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.progress.application.provided.LessonProgressUpdater;
import com.daesabu.meongcoach.progress.application.provided.TopicProgressUpdater;
import com.daesabu.meongcoach.progress.application.required.TopicProgressRepository;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.provided.dto.CurriculumDetailResult;
import com.daesabu.meongcoach.training.application.provided.dto.CurriculumListResult;
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
import com.daesabu.meongcoach.training.domain.exception.CurriculumNotFoundException;
import com.daesabu.meongcoach.training.domain.exception.TopicNotConfiguredException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class CurriculumFinderTest {

	private static final Long USER_ID = 42L;

	private static final Long OTHER_USER_ID = 99L;

	@Autowired
	private CurriculumFinder curriculumFinder;

	@Autowired
	private TopicProgressUpdater topicProgressUpdater;

	@Autowired
	private LessonProgressUpdater lessonProgressUpdater;

	@Autowired
	private TrainingCategoryRepository trainingCategoryRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private CurriculumRepository curriculumRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private TopicProgressRepository topicProgressRepository;

	@Autowired
	private EntitlementRepository entitlementRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 가장_최근에_진입한_토픽의_커리큘럼을_반환한다() {
		TrainingCategory category = saveCategory("기본 교육", 1);
		Topic first = saveTopic(category, "앉아", 1);
		Topic second = saveTopic(category, "기다려", 2);
		saveCurriculum(first, "앉아 1단계", 1);
		saveCurriculum(second, "기다려 1단계", 1);
		flushAndClear();
		topicProgressUpdater.enterTopic(USER_ID, second.getId());
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.topic().getId()).isEqualTo(second.getId());
		assertThat(curriculumList.curriculums()).extracting(Curriculum::getTitle)
				.containsExactly("기다려 1단계");
	}

	@Test
	void 진입_기록이_없으면_첫_토픽으로_폴백한다() {
		TrainingCategory advanced = saveCategory("심화 교육", 2);
		TrainingCategory basic = saveCategory("기본 교육", 1);
		Topic advancedTopic = saveTopic(advanced, "이리와", 1);
		Topic basicTopic = saveTopic(basic, "앉아", 1);
		saveCurriculum(advancedTopic, "이리와 1단계", 1);
		saveCurriculum(basicTopic, "앉아 1단계", 1);
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.topic().getId()).isEqualTo(basicTopic.getId());
		assertThat(curriculumList.curriculums()).extracting(Curriculum::getTitle)
				.containsExactly("앉아 1단계");
	}

	@Test
	void 진입_기록의_토픽이_더_이상_존재하지_않으면_첫_토픽으로_폴백한다() {
		TrainingCategory category = saveCategory("기본 교육", 1);
		Topic topic = saveTopic(category, "앉아", 1);
		saveCurriculum(topic, "앉아 1단계", 1);
		flushAndClear();
		topicProgressUpdater.enterTopic(USER_ID, 999L);
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.topic().getId()).isEqualTo(topic.getId());
	}

	@Test
	void 등록된_토픽이_하나도_없으면_예외를_던진다() {
		assertThatThrownBy(() -> curriculumFinder.findCurriculums(USER_ID))
				.isInstanceOf(TopicNotConfiguredException.class);
	}

	@Test
	void 커리큘럼을_정렬_순서_오름차순으로_반환한다() {
		Topic topic = saveTopicWithCategory();
		saveCurriculum(topic, "셋째 커리큘럼", 3);
		saveCurriculum(topic, "첫째 커리큘럼", 1);
		saveCurriculum(topic, "둘째 커리큘럼", 2);
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.curriculums()).extracting(Curriculum::getTitle)
				.containsExactly("첫째 커리큘럼", "둘째 커리큘럼", "셋째 커리큘럼");
	}

	@Test
	void 토픽의_레슨_중_사용자가_완료한_레슨_id를_반환한다() {
		Topic topic = saveTopicWithCategory();
		Curriculum first = saveCurriculum(topic, "1단계", 1);
		Curriculum second = saveCurriculum(topic, "2단계", 2);
		Lesson completed = saveLesson(first, "손 위의 간식", 1);
		saveLesson(first, "간식 없이 앉아", 2);
		saveLesson(second, "거리 두고 앉아", 1);
		flushAndClear();
		lessonProgressUpdater.updateCompletion(USER_ID, completed.getId());
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.completedLessonIds()).containsExactly(completed.getId());
	}

	@Test
	void 커리큘럼_목록에서_다른_사용자의_완료_기록은_제외한다() {
		Topic topic = saveTopicWithCategory();
		Curriculum curriculum = saveCurriculum(topic, "1단계", 1);
		Lesson lesson = saveLesson(curriculum, "손 위의 간식", 1);
		flushAndClear();
		lessonProgressUpdater.updateCompletion(OTHER_USER_ID, lesson.getId());
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.completedLessonIds()).isEmpty();
	}

	@Test
	void 레슨이_없는_커리큘럼도_목록에_포함한다() {
		Topic topic = saveTopicWithCategory();
		saveCurriculum(topic, "레슨 없는 커리큘럼", 1);
		Curriculum other = saveCurriculum(topic, "레슨 있는 커리큘럼", 2);
		saveLesson(other, "손 위의 간식", 1);
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.curriculums()).extracting(Curriculum::getTitle)
				.containsExactly("레슨 없는 커리큘럼", "레슨 있는 커리큘럼");
		assertThat(curriculumList.curriculums().getFirst().getLessons()).isEmpty();
	}

	@Test
	void 조회만_하고_진입_기록을_저장하지_않는다() {
		Topic topic = saveTopicWithCategory();
		saveCurriculum(topic, "1단계", 1);
		flushAndClear();

		curriculumFinder.findCurriculums(USER_ID);
		flushAndClear();

		assertThat(topicProgressRepository.count()).isZero();
	}

	@Test
	void 커리큘럼과_그_레슨_목록을_반환한다() {
		Topic topic = saveTopicWithCategory();
		Curriculum curriculum = saveCurriculum(topic, "앉아 2단계", 2);
		saveLesson(curriculum, "손 위의 간식", 1);
		saveLesson(curriculum, "간식 없이 앉아", 2);
		flushAndClear();

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, curriculum.getId());

		assertThat(detail.curriculum().getId()).isEqualTo(curriculum.getId());
		assertThat(detail.curriculum().getLessons()).extracting(Lesson::getTitle)
				.containsExactly("손 위의 간식", "간식 없이 앉아");
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

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, curriculum.getId());

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
		lessonProgressUpdater.updateCompletion(USER_ID, twice.getId());
		lessonProgressUpdater.updateCompletion(USER_ID, twice.getId());
		lessonProgressUpdater.updateCompletion(USER_ID, once.getId());
		flushAndClear();

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, curriculum.getId());

		assertThat(detail.completedCounts())
				.containsExactly(entry(twice.getId(), 2), entry(once.getId(), 1), entry(none.getId(), 0));
	}

	@Test
	void 커리큘럼_상세에서_다른_사용자의_완료_기록은_세지_않는다() {
		Topic topic = saveTopicWithCategory();
		Curriculum curriculum = saveCurriculum(topic, "1단계", 1);
		Lesson lesson = saveLesson(curriculum, "손 위의 간식", 1);
		flushAndClear();
		lessonProgressUpdater.updateCompletion(OTHER_USER_ID, lesson.getId());
		flushAndClear();

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, curriculum.getId());

		assertThat(detail.completedCounts()).containsExactly(entry(lesson.getId(), 0));
	}

	@Test
	void 존재하지_않는_커리큘럼이면_예외를_던진다() {
		assertThatThrownBy(() -> curriculumFinder.findCurriculum(USER_ID, 999L))
				.isInstanceOf(CurriculumNotFoundException.class);
	}

	@Test
	void 레슨이_없는_커리큘럼은_빈_레슨_목록을_반환한다() {
		Topic topic = saveTopicWithCategory();
		Curriculum empty = saveCurriculum(topic, "레슨 없는 커리큘럼", 1);
		Curriculum other = saveCurriculum(topic, "레슨 있는 커리큘럼", 2);
		saveLesson(other, "손 위의 간식", 1);
		flushAndClear();

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, empty.getId());

		assertThat(detail.curriculum().getLessons()).isEmpty();
		assertThat(detail.completedCounts()).isEmpty();
	}

	@Test
	void 이용권이_필요한_토픽에서_이용권이_없으면_보유하지_않았다고_반환한다() {
		Topic topic = saveTopic(savePaidCategory(EntitlementType.PUPPY), "앉아", 1);
		savePremiumCurriculum(topic, "앉아 1단계", 1);
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.hasEntitlement()).isFalse();
	}

	@Test
	void 이용권이_필요한_토픽에서_이용권이_있으면_보유했다고_반환한다() {
		Topic topic = saveTopic(savePaidCategory(EntitlementType.PUPPY), "앉아", 1);
		savePremiumCurriculum(topic, "앉아 1단계", 1);
		grant(USER_ID, EntitlementType.PUPPY);
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.hasEntitlement()).isTrue();
	}

	@Test
	void 이용권이_필요_없는_토픽이면_이용권이_없어도_보유했다고_반환한다() {
		Topic topic = saveTopicWithCategory();
		saveCurriculum(topic, "앉아 1단계", 1);
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.hasEntitlement()).isTrue();
	}

	@Test
	void 이용권_없이_유료_커리큘럼을_조회하면_예외를_던진다() {
		Topic topic = saveTopic(savePaidCategory(EntitlementType.PUPPY), "앉아", 1);
		Curriculum curriculum = savePremiumCurriculum(topic, "앉아 1단계", 1);
		grant(USER_ID, EntitlementType.JUNIOR);
		flushAndClear();

		assertThatThrownBy(() -> curriculumFinder.findCurriculum(USER_ID, curriculum.getId()))
				.isInstanceOf(EntitlementRequiredException.class);
	}

	@Test
	void 이용권이_있으면_유료_커리큘럼을_조회한다() {
		Topic topic = saveTopic(savePaidCategory(EntitlementType.PUPPY), "앉아", 1);
		Curriculum curriculum = savePremiumCurriculum(topic, "앉아 1단계", 1);
		grant(USER_ID, EntitlementType.PUPPY);
		flushAndClear();

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, curriculum.getId());

		assertThat(detail.curriculum().getId()).isEqualTo(curriculum.getId());
	}

	@Test
	void 유료_카테고리의_맛보기_커리큘럼은_이용권_없이_조회한다() {
		Topic topic = saveTopic(savePaidCategory(EntitlementType.PUPPY), "앉아", 1);
		Curriculum sample = saveCurriculum(topic, "앉아 맛보기", 1);
		flushAndClear();

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, sample.getId());

		assertThat(detail.curriculum().getId()).isEqualTo(sample.getId());
	}

	private TrainingCategory savePaidCategory(EntitlementType requiredEntitlementType) {
		return trainingCategoryRepository.save(
				TrainingCategoryFixture.create("퍼피 교육", 1, null, null, requiredEntitlementType));
	}

	private Curriculum savePremiumCurriculum(Topic topic, String title, int sortOrder) {
		return curriculumRepository.save(CurriculumFixture.create(topic, title, sortOrder, null, null, true));
	}

	private void grant(Long userId, EntitlementType type) {
		entitlementRepository.save(EntitlementFixture.create(userId, type, null));
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
}
