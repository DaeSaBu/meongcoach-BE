package com.daesabu.meongcoach.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.progress.application.LessonProgressService;
import com.daesabu.meongcoach.progress.application.TopicEntryService;
import com.daesabu.meongcoach.progress.application.provided.LessonProgressRecorder;
import com.daesabu.meongcoach.progress.application.provided.TopicEntryRecorder;
import com.daesabu.meongcoach.training.application.provided.CurriculumDetailView;
import com.daesabu.meongcoach.training.application.provided.CurriculumFinder;
import com.daesabu.meongcoach.training.application.provided.CurriculumListView;
import com.daesabu.meongcoach.training.application.provided.CurriculumView;
import com.daesabu.meongcoach.training.application.provided.LessonView;
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
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("커리큘럼 조회 서비스")
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
	@DisplayName("가장 최근에 진입한 토픽의 커리큘럼을 반환한다")
	void findCurriculumsReturnsCurriculumsOfLatestEnteredTopic() {
		TrainingCategory category = persistCategory("기본 교육", 1);
		Topic first = persistTopic(category, "앉아", 1);
		Topic second = persistTopic(category, "기다려", 2);
		persistCurriculum(first, "앉아 1단계", 1);
		persistCurriculum(second, "기다려 1단계", 1);
		flushAndClear();
		topicEntryRecorder.enterTopic(USER_ID, second.getId());
		flushAndClear();

		CurriculumListView curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.topicId()).isEqualTo(second.getId());
		assertThat(curriculumList.topicTitle()).isEqualTo("기다려");
		assertThat(curriculumList.curriculums()).extracting(CurriculumView::title)
				.containsExactly("기다려 1단계");
	}

	@Test
	@DisplayName("진입 기록이 없으면 첫 토픽으로 폴백한다")
	void findCurriculumsFallsBackToFirstTopicWhenNoEntryRecorded() {
		TrainingCategory advanced = persistCategory("심화 교육", 2);
		TrainingCategory basic = persistCategory("기본 교육", 1);
		Topic advancedTopic = persistTopic(advanced, "이리와", 1);
		Topic basicTopic = persistTopic(basic, "앉아", 1);
		persistCurriculum(advancedTopic, "이리와 1단계", 1);
		persistCurriculum(basicTopic, "앉아 1단계", 1);
		flushAndClear();

		CurriculumListView curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.topicId()).isEqualTo(basicTopic.getId());
		assertThat(curriculumList.topicTitle()).isEqualTo("앉아");
		assertThat(curriculumList.curriculums()).extracting(CurriculumView::title)
				.containsExactly("앉아 1단계");
	}

	@Test
	@DisplayName("진입 기록의 토픽이 더 이상 존재하지 않으면 첫 토픽으로 폴백한다")
	void findCurriculumsFallsBackToFirstTopicWhenEnteredTopicIsGone() {
		TrainingCategory category = persistCategory("기본 교육", 1);
		Topic topic = persistTopic(category, "앉아", 1);
		persistCurriculum(topic, "앉아 1단계", 1);
		flushAndClear();
		topicEntryRecorder.enterTopic(USER_ID, 999L);
		flushAndClear();

		CurriculumListView curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.topicId()).isEqualTo(topic.getId());
		assertThat(curriculumList.topicTitle()).isEqualTo("앉아");
	}

	@Test
	@DisplayName("등록된 토픽이 하나도 없으면 예외를 던진다")
	void findCurriculumsThrowsWhenNoTopicIsConfigured() {
		assertThatThrownBy(() -> curriculumFinder.findCurriculums(USER_ID))
				.isInstanceOf(TopicNotConfiguredException.class);
	}

	@Test
	@DisplayName("커리큘럼을 정렬 순서 오름차순으로 반환한다")
	void findCurriculumsOrdersCurriculumsBySortOrder() {
		Topic topic = persistTopicWithCategory();
		persistCurriculum(topic, "셋째 커리큘럼", 3);
		persistCurriculum(topic, "첫째 커리큘럼", 1);
		persistCurriculum(topic, "둘째 커리큘럼", 2);
		flushAndClear();

		CurriculumListView curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.curriculums()).extracting(CurriculumView::title)
				.containsExactly("첫째 커리큘럼", "둘째 커리큘럼", "셋째 커리큘럼");
	}

	@Test
	@DisplayName("커리큘럼마다 전체 레슨 수와 완료한 레슨 수를 센다")
	void findCurriculumsCountsTotalAndCompletedLessons() {
		Topic topic = persistTopicWithCategory();
		Curriculum first = persistCurriculum(topic, "1단계", 1);
		Curriculum second = persistCurriculum(topic, "2단계", 2);
		Lesson completed = persistLesson(first, "손 위의 간식", 1);
		persistLesson(first, "간식 없이 앉아", 2);
		persistLesson(second, "거리 두고 앉아", 1);
		flushAndClear();
		lessonProgressRecorder.completeLesson(USER_ID, completed.getId());
		flushAndClear();

		CurriculumListView curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.curriculums()).extracting(CurriculumView::totalLessons)
				.containsExactly(2, 1);
		assertThat(curriculumList.curriculums()).extracting(CurriculumView::completedLessons)
				.containsExactly(1, 0);
		assertThat(curriculumList.curriculums()).extracting(CurriculumView::status)
				.containsExactly(CurriculumStatus.IN_PROGRESS, CurriculumStatus.NOT_STARTED);
	}

	@Test
	@DisplayName("다른 사용자의 완료 기록은 세지 않는다")
	void findCurriculumsIgnoresOtherUsersCompletion() {
		Topic topic = persistTopicWithCategory();
		Curriculum curriculum = persistCurriculum(topic, "1단계", 1);
		Lesson lesson = persistLesson(curriculum, "손 위의 간식", 1);
		flushAndClear();
		lessonProgressRecorder.completeLesson(OTHER_USER_ID, lesson.getId());
		flushAndClear();

		CurriculumListView curriculumList = curriculumFinder.findCurriculums(USER_ID);

		assertThat(curriculumList.curriculums().getFirst().completedLessons()).isZero();
		assertThat(curriculumList.curriculums().getFirst().status()).isEqualTo(CurriculumStatus.NOT_STARTED);
	}

	@Test
	@DisplayName("레슨이 없는 커리큘럼은 시작 전 상태로 반환한다")
	void findCurriculumsReturnsNotStartedWhenCurriculumHasNoLesson() {
		Topic topic = persistTopicWithCategory();
		persistCurriculum(topic, "레슨 없는 커리큘럼", 1);
		Curriculum other = persistCurriculum(topic, "레슨 있는 커리큘럼", 2);
		persistLesson(other, "손 위의 간식", 1);
		flushAndClear();

		CurriculumListView curriculumList = curriculumFinder.findCurriculums(USER_ID);

		CurriculumView empty = curriculumList.curriculums().getFirst();
		assertThat(empty.totalLessons()).isZero();
		assertThat(empty.completedLessons()).isZero();
		assertThat(empty.status()).isEqualTo(CurriculumStatus.NOT_STARTED);
	}

	@Test
	@DisplayName("모든 레슨을 완료한 커리큘럼은 완료 상태로 반환한다")
	void findCurriculumsReturnsCompletedWhenEveryLessonIsCompleted() {
		Topic topic = persistTopicWithCategory();
		Curriculum curriculum = persistCurriculum(topic, "1단계", 1);
		Lesson first = persistLesson(curriculum, "손 위의 간식", 1);
		Lesson second = persistLesson(curriculum, "간식 없이 앉아", 2);
		flushAndClear();
		lessonProgressRecorder.completeLesson(USER_ID, first.getId());
		lessonProgressRecorder.completeLesson(USER_ID, second.getId());
		flushAndClear();

		CurriculumListView curriculumList = curriculumFinder.findCurriculums(USER_ID);

		CurriculumView view = curriculumList.curriculums().getFirst();
		assertThat(view.totalLessons()).isEqualTo(2);
		assertThat(view.completedLessons()).isEqualTo(2);
		assertThat(view.status()).isEqualTo(CurriculumStatus.COMPLETED);
	}

	@Test
	@DisplayName("커리큘럼 수와 무관하게 다섯 번의 쿼리로 조회한다")
	void findCurriculumsExecutesConstantQueryCount() {
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
	@DisplayName("조회만 하고 진입 기록을 저장하지 않는다")
	void findCurriculumsDoesNotStoreAnyData() {
		Topic topic = persistTopicWithCategory();
		persistCurriculum(topic, "1단계", 1);
		flushAndClear();

		curriculumFinder.findCurriculums(USER_ID);
		flushAndClear();

		assertThat(countTopicEntries()).isZero();
	}

	@Test
	@DisplayName("커리큘럼과 그 레슨 목록을 반환한다")
	void findCurriculumReturnsCurriculumWithLessons() {
		Topic topic = persistTopicWithCategory();
		Curriculum curriculum = persistCurriculum(topic, "앉아 2단계", 2);
		persistLesson(curriculum, "손 위의 간식", 1, 5);
		persistLesson(curriculum, "간식 없이 앉아", 2, 10);
		flushAndClear();

		CurriculumDetailView detail = curriculumFinder.findCurriculum(USER_ID, curriculum.getId());

		assertThat(detail.id()).isEqualTo(curriculum.getId());
		assertThat(detail.topicId()).isEqualTo(topic.getId());
		assertThat(detail.title()).isEqualTo("앉아 2단계");
		assertThat(detail.sortOrder()).isEqualTo(2);
		assertThat(detail.lessons()).extracting(LessonView::title)
				.containsExactly("손 위의 간식", "간식 없이 앉아");
		assertThat(detail.lessons()).extracting(LessonView::estimatedMinutes)
				.containsExactly(5, 10);
	}

	@Test
	@DisplayName("레슨을 정렬 순서 오름차순으로 반환하고 다른 커리큘럼의 레슨은 제외한다")
	void findCurriculumOrdersLessonsBySortOrder() {
		Topic topic = persistTopicWithCategory();
		Curriculum curriculum = persistCurriculum(topic, "1단계", 1);
		Curriculum other = persistCurriculum(topic, "2단계", 2);
		persistLesson(curriculum, "셋째 레슨", 3, 5);
		persistLesson(curriculum, "첫째 레슨", 1, 5);
		persistLesson(curriculum, "둘째 레슨", 2, 5);
		persistLesson(other, "다른 커리큘럼 레슨", 1, 5);
		flushAndClear();

		CurriculumDetailView detail = curriculumFinder.findCurriculum(USER_ID, curriculum.getId());

		assertThat(detail.lessons()).extracting(LessonView::title)
				.containsExactly("첫째 레슨", "둘째 레슨", "셋째 레슨");
	}

	@Test
	@DisplayName("레슨마다 사용자의 반복 완료 횟수를 반환하고 기록이 없으면 0으로 채운다")
	void findCurriculumReturnsCompletedCountOfEachLesson() {
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

		CurriculumDetailView detail = curriculumFinder.findCurriculum(USER_ID, curriculum.getId());

		assertThat(detail.lessons()).extracting(LessonView::completedCount)
				.containsExactly(2, 1, 0);
	}

	@Test
	@DisplayName("다른 사용자의 완료 기록은 세지 않는다")
	void findCurriculumIgnoresOtherUsersProgress() {
		Topic topic = persistTopicWithCategory();
		Curriculum curriculum = persistCurriculum(topic, "1단계", 1);
		Lesson lesson = persistLesson(curriculum, "손 위의 간식", 1, 5);
		flushAndClear();
		lessonProgressRecorder.completeLesson(OTHER_USER_ID, lesson.getId());
		flushAndClear();

		CurriculumDetailView detail = curriculumFinder.findCurriculum(USER_ID, curriculum.getId());

		assertThat(detail.lessons().getFirst().completedCount()).isZero();
	}

	@Test
	@DisplayName("존재하지 않는 커리큘럼이면 예외를 던진다")
	void findCurriculumThrowsWhenCurriculumDoesNotExist() {
		assertThatThrownBy(() -> curriculumFinder.findCurriculum(USER_ID, 999L))
				.isInstanceOf(CurriculumNotFoundException.class);
	}

	@Test
	@DisplayName("레슨이 없는 커리큘럼은 빈 레슨 목록을 반환한다")
	void findCurriculumReturnsEmptyLessonsWhenCurriculumHasNoLesson() {
		Topic topic = persistTopicWithCategory();
		Curriculum empty = persistCurriculum(topic, "레슨 없는 커리큘럼", 1);
		Curriculum other = persistCurriculum(topic, "레슨 있는 커리큘럼", 2);
		persistLesson(other, "손 위의 간식", 1, 5);
		flushAndClear();

		CurriculumDetailView detail = curriculumFinder.findCurriculum(USER_ID, empty.getId());

		assertThat(detail.id()).isEqualTo(empty.getId());
		assertThat(detail.lessons()).isEmpty();
	}

	@Test
	@DisplayName("레슨 수와 무관하게 세 번의 쿼리로 조회한다")
	void findCurriculumExecutesConstantQueryCount() {
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
