package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.provided.dto.TopicProgressResult;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumFixture;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonFixture;
import com.daesabu.meongcoach.training.domain.LessonProgress;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import com.daesabu.meongcoach.training.domain.shared.ProgressStatus;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

@ApplicationTest
class TopicProgressFinderTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private TopicProgressFinder finder;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 토픽을_교육_순서로_반환하고_카테고리와_썸네일을_함께_반환한다() {
		TrainingCategory later = saveCategory("성견", 2);
		Topic last = saveTopic(later, "앉아", 1);
		saveLessons(saveCurriculum(last, 1), 1);
		TrainingCategory first = saveCategory("기본기", 1);
		Topic second = saveTopic(first, "배변", 2);
		saveLessons(saveCurriculum(second, 1), 1);
		Topic firstTopic = saveTopic(first, "간식", 1);
		ReflectionTestUtils.setField(firstTopic, "thumbnailUrl", "https://cdn.test/1601.jpg");
		saveLessons(saveCurriculum(firstTopic, 1), 1);
		flushAndClear();

		List<TopicProgressResult> result = finder.findAll(USER_ID);

		assertThat(result).extracting(TopicProgressResult::id)
				.containsExactly(firstTopic.getId(), second.getId(), last.getId());
		TopicProgressResult head = result.getFirst();
		assertThat(head.categoryId()).isEqualTo(first.getId());
		assertThat(head.categoryTitle()).isEqualTo("기본기");
		assertThat(head.categorySortOrder()).isEqualTo(1);
		assertThat(result.getLast().categorySortOrder()).isEqualTo(2);
		assertThat(head.thumbnailUrl()).isEqualTo("https://cdn.test/1601.jpg");
		assertThat(head.hasThumbnail()).isTrue();
		assertThat(result.get(1).thumbnailUrl()).isNull();
		assertThat(result.get(1).hasThumbnail()).isFalse();
	}

	@Test
	void 본인이_완료한_레슨_수와_완료하지_않은_첫_커리큘럼을_반환한다() {
		Topic topic = saveTopic(saveCategory("기본기", 1), "간식", 1);
		Curriculum done = saveCurriculum(topic, 1);
		List<Lesson> doneLessons = saveLessons(done, 2);
		Curriculum next = saveCurriculum(topic, 2);
		List<Lesson> nextLessons = saveLessons(next, 2);
		complete(USER_ID, doneLessons.get(0));
		complete(USER_ID, doneLessons.get(1));
		complete(USER_ID, nextLessons.get(0));
		complete(43L, nextLessons.get(1));
		flushAndClear();

		TopicProgressResult result = finder.findAll(USER_ID).getFirst();

		assertThat(result.totalLessons()).isEqualTo(4);
		assertThat(result.completedLessons()).isEqualTo(3);
		assertThat(result.status()).isEqualTo(ProgressStatus.IN_PROGRESS);
		assertThat(result.nextCurriculumId()).isEqualTo(next.getId());
	}

	@Test
	void 모든_레슨을_완료한_토픽은_첫_커리큘럼을_다음_커리큘럼으로_반환한다() {
		Topic topic = saveTopic(saveCategory("기본기", 1), "간식", 1);
		Curriculum first = saveCurriculum(topic, 1);
		complete(USER_ID, saveLessons(first, 1).getFirst());
		Curriculum second = saveCurriculum(topic, 2);
		complete(USER_ID, saveLessons(second, 1).getFirst());
		flushAndClear();

		TopicProgressResult result = finder.findAll(USER_ID).getFirst();

		assertThat(result.completedLessons()).isEqualTo(2);
		assertThat(result.nextCurriculumId()).isEqualTo(first.getId());
	}

	@Test
	void 학습_기록이_없으면_마지막_학습_시각이_없다() {
		Topic topic = saveTopic(saveCategory("기본기", 1), "간식", 1);
		Curriculum curriculum = saveCurriculum(topic, 1);
		saveLessons(curriculum, 1);
		flushAndClear();

		TopicProgressResult result = finder.findAll(USER_ID).getFirst();

		assertThat(result.completedLessons()).isZero();
		assertThat(result.nextCurriculumId()).isEqualTo(curriculum.getId());
		assertThat(result.lastStudiedAt()).isNull();
	}

	@Test
	void 마지막_학습_시각은_토픽에서_가장_최근에_완료한_레슨_기준이다() {
		Topic topic = saveTopic(saveCategory("기본기", 1), "간식", 1);
		List<Lesson> lessons = saveLessons(saveCurriculum(topic, 1), 2);
		LessonProgress older = complete(USER_ID, lessons.get(0));
		LessonProgress latest = complete(USER_ID, lessons.get(1));
		flushAndClear();
		setUpdatedAt(older, LocalDateTime.of(2026, 11, 1, 9, 0));
		setUpdatedAt(latest, LocalDateTime.of(2026, 11, 7, 21, 30));

		TopicProgressResult result = finder.findAll(USER_ID).getFirst();

		assertThat(result.lastStudiedAt()).isEqualTo(LocalDateTime.of(2026, 11, 7, 21, 30));
	}

	@Test
	void 레슨이_없는_토픽은_반환하지_않는다() {
		TrainingCategory category = saveCategory("기본기", 1);
		Topic empty = saveTopic(category, "빈 토픽", 1);
		saveCurriculum(empty, 1);
		saveTopic(category, "커리큘럼 없는 토픽", 2);
		Topic topic = saveTopic(category, "간식", 3);
		saveLessons(saveCurriculum(topic, 1), 1);
		flushAndClear();

		List<TopicProgressResult> result = finder.findAll(USER_ID);

		assertThat(result).extracting(TopicProgressResult::id).containsExactly(topic.getId());
	}

	private TrainingCategory saveCategory(String title, int sortOrder) {
		TrainingCategory category = TrainingCategoryFixture.create(title, sortOrder, null, null);
		entityManager.persist(category);
		return category;
	}

	private Topic saveTopic(TrainingCategory category, String title, int sortOrder) {
		Topic topic = TopicFixture.create(category, title, sortOrder, title + " 설명", null, null);
		entityManager.persist(topic);
		return topic;
	}

	private Curriculum saveCurriculum(Topic topic, int sortOrder) {
		Curriculum curriculum = CurriculumFixture.create(topic, "커리큘럼 " + sortOrder, sortOrder, null, null);
		entityManager.persist(curriculum);
		return curriculum;
	}

	private List<Lesson> saveLessons(Curriculum curriculum, int count) {
		List<Lesson> lessons = new ArrayList<>();
		for (int order = 1; order <= count; order++) {
			Lesson lesson = LessonFixture.create(curriculum, "레슨 " + order, order, 5);
			entityManager.persist(lesson);
			lessons.add(lesson);
		}
		return lessons;
	}

	private LessonProgress complete(Long userId, Lesson lesson) {
		LessonProgress progress = LessonProgress.start(userId, lesson.getId());
		progress.complete();
		entityManager.persist(progress);
		return progress;
	}

	private void setUpdatedAt(LessonProgress progress, LocalDateTime updatedAt) {
		entityManager.createNativeQuery("UPDATE lesson_progress SET updated_at = :updatedAt WHERE id = :id")
				.setParameter("updatedAt", updatedAt)
				.setParameter("id", progress.getId())
				.executeUpdate();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
