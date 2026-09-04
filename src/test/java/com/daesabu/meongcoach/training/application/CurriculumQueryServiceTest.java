package com.daesabu.meongcoach.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.progress.application.LessonProgressService;
import com.daesabu.meongcoach.progress.application.TopicEntryService;
import com.daesabu.meongcoach.progress.application.provided.LessonProgressRecorder;
import com.daesabu.meongcoach.progress.application.provided.TopicEntryRecorder;
import com.daesabu.meongcoach.training.application.provided.CurriculumDetailResult;
import com.daesabu.meongcoach.training.application.provided.CurriculumFinder;
import com.daesabu.meongcoach.training.application.provided.CurriculumListResult;
import com.daesabu.meongcoach.training.application.provided.CurriculumResult;
import com.daesabu.meongcoach.training.application.provided.LessonResult;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumCreateCommand;
import com.daesabu.meongcoach.training.domain.CurriculumStatus;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonCreateCommand;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicCreateCommand;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.exception.CurriculumNotFoundException;
import com.daesabu.meongcoach.training.domain.exception.TopicNotConfiguredException;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * 커리큘럼 리스트·세부 조회 서비스 검증.
 */
@DataJpaTest
@Import({CurriculumQueryService.class, TopicEntryService.class, LessonProgressService.class})
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class CurriculumQueryServiceTest {

	private static final Long USER_ID = 42L;

	private static final Long OTHER_USER_ID = 99L;

	@Autowired
	private CurriculumFinder curriculumFinder;

	@Autowired
	private TopicEntryRecorder topicEntryRecorder;

	@Autowired
	private LessonProgressRecorder lessonProgressRecorder;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Test
	void 가장_최근에_진입한_토픽의_커리큘럼을_반환한다() {
		TrainingCategory category = persistCategory("기본 교육", 1);
		Topic first = persistTopic(category, "앉아", 1);
		Topic second = persistTopic(category, "기다려", 2);
		persistCurriculum(first, "앉아 1단계", 1);
		persistCurriculum(second, "기다려 1단계", 1);
		flushAndClear();
		topicEntryRecorder.enterTopic(USER_ID, second.getId());
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.topicId()).isEqualTo(second.getId());
		assertThat(curriculumList.topicTitle()).isEqualTo("기다려");
		assertThat(curriculumList.curriculums()).extracting(CurriculumResult::title)
				.containsExactly("기다려 1단계");
	}

	@Test
	void 진입_기록이_없으면_첫_토픽으로_폴백한다() {
		TrainingCategory advanced = persistCategory("심화 교육", 2);
		TrainingCategory basic = persistCategory("기본 교육", 1);
		Topic advancedTopic = persistTopic(advanced, "이리와", 1);
		Topic basicTopic = persistTopic(basic, "앉아", 1);
		persistCurriculum(advancedTopic, "이리와 1단계", 1);
		persistCurriculum(basicTopic, "앉아 1단계", 1);
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.topicId()).isEqualTo(basicTopic.getId());
		assertThat(curriculumList.topicTitle()).isEqualTo("앉아");
		assertThat(curriculumList.curriculums()).extracting(CurriculumResult::title)
				.containsExactly("앉아 1단계");
	}

	@Test
	void 진입_기록의_토픽이_더_이상_존재하지_않으면_첫_토픽으로_폴백한다() {
		TrainingCategory category = persistCategory("기본 교육", 1);
		Topic topic = persistTopic(category, "앉아", 1);
		persistCurriculum(topic, "앉아 1단계", 1);
		flushAndClear();
		topicEntryRecorder.enterTopic(USER_ID, 999L);
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.topicId()).isEqualTo(topic.getId());
		assertThat(curriculumList.topicTitle()).isEqualTo("앉아");
	}

	@Test
	void 등록된_토픽이_하나도_없으면_예외를_던진다() {
		assertThatThrownBy(() -> curriculumFinder.findCurriculums(USER_ID))
				.isInstanceOf(TopicNotConfiguredException.class);
	}

	@Test
	void 커리큘럼을_정렬_순서_오름차순으로_반환한다() {
		Topic topic = persistTopicWithCategory();
		persistCurriculum(topic, "셋째 커리큘럼", 3);
		persistCurriculum(topic, "첫째 커리큘럼", 1);
		persistCurriculum(topic, "둘째 커리큘럼", 2);
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.curriculums()).extracting(CurriculumResult::title)
				.containsExactly("첫째 커리큘럼", "둘째 커리큘럼", "셋째 커리큘럼");
	}

	@Test
	void 커리큘럼마다_전체_레슨_수와_완료한_레슨_수를_센다() {
		Topic topic = persistTopicWithCategory();
		Curriculum first = persistCurriculum(topic, "1단계", 1);
		Curriculum second = persistCurriculum(topic, "2단계", 2);
		Lesson completed = persistLesson(first, "손 위의 간식", 1);
		persistLesson(first, "간식 없이 앉아", 2);
		persistLesson(second, "거리 두고 앉아", 1);
		flushAndClear();
		lessonProgressRecorder.completeLesson(USER_ID, completed.getId());
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.curriculums()).extracting(CurriculumResult::totalLessons)
				.containsExactly(2, 1);
		assertThat(curriculumList.curriculums()).extracting(CurriculumResult::completedLessons)
				.containsExactly(1, 0);
		assertThat(curriculumList.curriculums()).extracting(CurriculumResult::status)
				.containsExactly(CurriculumStatus.IN_PROGRESS, CurriculumStatus.NOT_STARTED);
	}

	@Test
	void 커리큘럼_목록에서_다른_사용자의_완료_기록은_세지_않는다() {
		Topic topic = persistTopicWithCategory();
		Curriculum curriculum = persistCurriculum(topic, "1단계", 1);
		Lesson lesson = persistLesson(curriculum, "손 위의 간식", 1);
		flushAndClear();
		lessonProgressRecorder.completeLesson(OTHER_USER_ID, lesson.getId());
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.curriculums().getFirst().completedLessons()).isZero();
		assertThat(curriculumList.curriculums().getFirst().status()).isEqualTo(CurriculumStatus.NOT_STARTED);
	}

	@Test
	void 레슨이_없는_커리큘럼은_시작_전_상태로_반환한다() {
		Topic topic = persistTopicWithCategory();
		persistCurriculum(topic, "레슨 없는 커리큘럼", 1);
		Curriculum other = persistCurriculum(topic, "레슨 있는 커리큘럼", 2);
		persistLesson(other, "손 위의 간식", 1);
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID);

		CurriculumResult empty = curriculumList.curriculums().getFirst();
		assertThat(empty.totalLessons()).isZero();
		assertThat(empty.completedLessons()).isZero();
		assertThat(empty.status()).isEqualTo(CurriculumStatus.NOT_STARTED);
	}

	@Test
	void 모든_레슨을_완료한_커리큘럼은_완료_상태로_반환한다() {
		Topic topic = persistTopicWithCategory();
		Curriculum curriculum = persistCurriculum(topic, "1단계", 1);
		Lesson first = persistLesson(curriculum, "손 위의 간식", 1);
		Lesson second = persistLesson(curriculum, "간식 없이 앉아", 2);
		flushAndClear();
		lessonProgressRecorder.completeLesson(USER_ID, first.getId());
		lessonProgressRecorder.completeLesson(USER_ID, second.getId());
		flushAndClear();

		CurriculumListResult curriculumList = curriculumFinder.findCurriculums(USER_ID);

		CurriculumResult result = curriculumList.curriculums().getFirst();
		assertThat(result.totalLessons()).isEqualTo(2);
		assertThat(result.completedLessons()).isEqualTo(2);
		assertThat(result.status()).isEqualTo(CurriculumStatus.COMPLETED);
	}

	@Test
	void 커리큘럼_수와_무관하게_다섯_번의_쿼리로_조회한다() {
		Topic topic = persistTopicWithCategory();
		Curriculum first = persistCurriculum(topic, "1단계", 1);
		Curriculum second = persistCurriculum(topic, "2단계", 2);
		Curriculum third = persistCurriculum(topic, "3단계", 3);
		persistLesson(first, "첫째 레슨", 1);
		persistLesson(second, "둘째 레슨", 1);
		persistLesson(third, "셋째 레슨", 1);
		flushAndClear();
		topicEntryRecorder.enterTopic(USER_ID, topic.getId());
		flushAndClear();
		Statistics statistics = clearedStatistics();

		curriculumFinder.findCurriculums(USER_ID);

		assertThat(statistics.getPrepareStatementCount()).isEqualTo(5);
	}

	@Test
	void 조회만_하고_진입_기록을_저장하지_않는다() {
		Topic topic = persistTopicWithCategory();
		persistCurriculum(topic, "1단계", 1);
		flushAndClear();

		curriculumFinder.findCurriculums(USER_ID);
		flushAndClear();

		assertThat(countTopicEntries()).isZero();
	}

	@Test
	void 커리큘럼과_그_레슨_목록을_반환한다() {
		Topic topic = persistTopicWithCategory();
		Curriculum curriculum = persistCurriculum(topic, "앉아 2단계", 2);
		persistLesson(curriculum, "손 위의 간식", 1, 5);
		persistLesson(curriculum, "간식 없이 앉아", 2, 10);
		flushAndClear();

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, curriculum.getId());

		assertThat(detail.id()).isEqualTo(curriculum.getId());
		assertThat(detail.topicId()).isEqualTo(topic.getId());
		assertThat(detail.title()).isEqualTo("앉아 2단계");
		assertThat(detail.sortOrder()).isEqualTo(2);
		assertThat(detail.lessons()).extracting(LessonResult::title)
				.containsExactly("손 위의 간식", "간식 없이 앉아");
		assertThat(detail.lessons()).extracting(LessonResult::estimatedMinutes)
				.containsExactly(5, 10);
	}

	@Test
	void 레슨을_정렬_순서_오름차순으로_반환하고_다른_커리큘럼의_레슨은_제외한다() {
		Topic topic = persistTopicWithCategory();
		Curriculum curriculum = persistCurriculum(topic, "1단계", 1);
		Curriculum other = persistCurriculum(topic, "2단계", 2);
		persistLesson(curriculum, "셋째 레슨", 3, 5);
		persistLesson(curriculum, "첫째 레슨", 1, 5);
		persistLesson(curriculum, "둘째 레슨", 2, 5);
		persistLesson(other, "다른 커리큘럼 레슨", 1, 5);
		flushAndClear();

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, curriculum.getId());

		assertThat(detail.lessons()).extracting(LessonResult::title)
				.containsExactly("첫째 레슨", "둘째 레슨", "셋째 레슨");
	}

	@Test
	void 레슨마다_사용자의_반복_완료_횟수를_반환하고_기록이_없으면_0으로_채운다() {
		Topic topic = persistTopicWithCategory();
		Curriculum curriculum = persistCurriculum(topic, "1단계", 1);
		Lesson twice = persistLesson(curriculum, "첫째 레슨", 1, 5);
		Lesson once = persistLesson(curriculum, "둘째 레슨", 2, 5);
		persistLesson(curriculum, "셋째 레슨", 3, 5);
		flushAndClear();
		lessonProgressRecorder.completeLesson(USER_ID, twice.getId());
		lessonProgressRecorder.completeLesson(USER_ID, twice.getId());
		lessonProgressRecorder.completeLesson(USER_ID, once.getId());
		flushAndClear();

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, curriculum.getId());

		assertThat(detail.lessons()).extracting(LessonResult::completedCount)
				.containsExactly(2, 1, 0);
	}

	@Test
	void 커리큘럼_상세에서_다른_사용자의_완료_기록은_세지_않는다() {
		Topic topic = persistTopicWithCategory();
		Curriculum curriculum = persistCurriculum(topic, "1단계", 1);
		Lesson lesson = persistLesson(curriculum, "손 위의 간식", 1, 5);
		flushAndClear();
		lessonProgressRecorder.completeLesson(OTHER_USER_ID, lesson.getId());
		flushAndClear();

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, curriculum.getId());

		assertThat(detail.lessons().getFirst().completedCount()).isZero();
	}

	@Test
	void 존재하지_않는_커리큘럼이면_예외를_던진다() {
		assertThatThrownBy(() -> curriculumFinder.findCurriculum(USER_ID, 999L))
				.isInstanceOf(CurriculumNotFoundException.class);
	}

	@Test
	void 레슨이_없는_커리큘럼은_빈_레슨_목록을_반환한다() {
		Topic topic = persistTopicWithCategory();
		Curriculum empty = persistCurriculum(topic, "레슨 없는 커리큘럼", 1);
		Curriculum other = persistCurriculum(topic, "레슨 있는 커리큘럼", 2);
		persistLesson(other, "손 위의 간식", 1, 5);
		flushAndClear();

		CurriculumDetailResult detail = curriculumFinder.findCurriculum(USER_ID, empty.getId());

		assertThat(detail.id()).isEqualTo(empty.getId());
		assertThat(detail.lessons()).isEmpty();
	}

	@Test
	void 레슨_수와_무관하게_세_번의_쿼리로_조회한다() {
		Topic topic = persistTopicWithCategory();
		Curriculum curriculum = persistCurriculum(topic, "1단계", 1);
		persistLesson(curriculum, "첫째 레슨", 1, 5);
		persistLesson(curriculum, "둘째 레슨", 2, 5);
		persistLesson(curriculum, "셋째 레슨", 3, 5);
		flushAndClear();
		Statistics statistics = clearedStatistics();

		curriculumFinder.findCurriculum(USER_ID, curriculum.getId());

		assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
	}

	private TrainingCategory persistCategory(String title, int sortOrder) {
		return entityManager.persist(TrainingCategory.create(title, sortOrder, null, null));
	}

	private Topic persistTopic(TrainingCategory category, String title, int sortOrder) {
		return entityManager.persist(Topic.create(category, new TopicCreateCommand(title, sortOrder, null, null, null)));
	}

	private Topic persistTopicWithCategory() {
		return persistTopic(persistCategory("기본 교육", 1), "앉아", 1);
	}

	private Curriculum persistCurriculum(Topic topic, String title, int sortOrder) {
		CurriculumCreateCommand command = new CurriculumCreateCommand(title, sortOrder, null, null);
		return entityManager.persist(Curriculum.create(topic, command));
	}

	private Lesson persistLesson(Curriculum curriculum, String title, int sortOrder) {
		return persistLesson(curriculum, title, sortOrder, 5);
	}

	private Lesson persistLesson(Curriculum curriculum, String title, int sortOrder, int estimatedMinutes) {
		LessonCreateCommand command = new LessonCreateCommand(title, sortOrder, estimatedMinutes);
		return entityManager.persist(Lesson.create(curriculum, command));
	}

	private long countTopicEntries() {
		return entityManager.getEntityManager()
				.createQuery("select count(c) from UserSelectedTopic c", Long.class)
				.getSingleResult();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	private Statistics clearedStatistics() {
		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.clear();
		return statistics;
	}
}
