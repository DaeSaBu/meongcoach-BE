package com.daesabu.meongcoach.training.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumCreateCommand;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonCreateCommand;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicCreateCommand;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * 레슨 조회 리포지토리 검증.
 */
@DataJpaTest
class LessonRepositoryTest {

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 여러_커리큘럼의_레슨을_정렬_순서_오름차순으로_조회한다() {
		Topic topic = persistTopic("기본 교육");
		Curriculum first = persistCurriculum(topic, "첫째 커리큘럼", 1);
		Curriculum second = persistCurriculum(topic, "둘째 커리큘럼", 2);
		persistLesson(first, "셋째", 3);
		persistLesson(second, "둘째", 2);
		persistLesson(first, "첫째", 1);
		entityManager.flush();

		List<Lesson> lessons = lessonRepository
				.findAllByCurriculum_IdInOrderBySortOrderAscIdAsc(List.of(first.getId(), second.getId()));

		assertThat(lessons).extracting(Lesson::getTitle)
				.containsExactly("첫째", "둘째", "셋째");
	}

	@Test
	void 정렬_순서가_같으면_id_오름차순으로_조회한다() {
		Topic topic = persistTopic("기본 교육");
		Curriculum curriculum = persistCurriculum(topic, "커리큘럼", 1);
		Lesson first = persistLesson(curriculum, "먼저 등록", 1);
		Lesson second = persistLesson(curriculum, "나중 등록", 1);
		entityManager.flush();

		List<Lesson> lessons = lessonRepository
				.findAllByCurriculum_IdInOrderBySortOrderAscIdAsc(List.of(curriculum.getId()));

		assertThat(lessons).extracting(Lesson::getId)
				.containsExactly(first.getId(), second.getId());
	}

	@Test
	void 조회_대상에_없는_커리큘럼의_레슨은_조회되지_않는다() {
		Topic topic = persistTopic("기본 교육");
		Curriculum target = persistCurriculum(topic, "대상 커리큘럼", 1);
		Curriculum other = persistCurriculum(topic, "다른 커리큘럼", 2);
		persistLesson(target, "대상 레슨", 1);
		persistLesson(other, "다른 레슨", 1);
		entityManager.flush();

		List<Lesson> lessons = lessonRepository
				.findAllByCurriculum_IdInOrderBySortOrderAscIdAsc(List.of(target.getId()));

		assertThat(lessons).extracting(Lesson::getTitle)
				.containsExactly("대상 레슨");
	}

	@Test
	void 커리큘럼_id_목록이_비어_있으면_빈_목록을_반환한다() {
		Topic topic = persistTopic("기본 교육");
		Curriculum curriculum = persistCurriculum(topic, "커리큘럼", 1);
		persistLesson(curriculum, "레슨", 1);
		entityManager.flush();

		List<Lesson> lessons = lessonRepository.findAllByCurriculum_IdInOrderBySortOrderAscIdAsc(List.of());

		assertThat(lessons).isEmpty();
	}

	@Test
	void 단일_커리큘럼의_레슨을_정렬_순서_오름차순으로_조회한다() {
		Topic topic = persistTopic("기본 교육");
		Curriculum curriculum = persistCurriculum(topic, "커리큘럼", 1);
		persistLesson(curriculum, "셋째", 3);
		persistLesson(curriculum, "첫째", 1);
		persistLesson(curriculum, "둘째", 2);
		entityManager.flush();

		List<Lesson> lessons = lessonRepository.findAllByCurriculum_IdOrderBySortOrderAscIdAsc(curriculum.getId());

		assertThat(lessons).extracting(Lesson::getTitle)
				.containsExactly("첫째", "둘째", "셋째");
	}

	@Test
	void 다른_커리큘럼의_레슨은_조회되지_않는다() {
		Topic topic = persistTopic("기본 교육");
		Curriculum target = persistCurriculum(topic, "대상 커리큘럼", 1);
		Curriculum other = persistCurriculum(topic, "다른 커리큘럼", 2);
		persistLesson(target, "대상 레슨", 1);
		persistLesson(other, "다른 레슨", 1);
		entityManager.flush();

		List<Lesson> lessons = lessonRepository.findAllByCurriculum_IdOrderBySortOrderAscIdAsc(target.getId());

		assertThat(lessons).extracting(Lesson::getTitle)
				.containsExactly("대상 레슨");
	}

	@Test
	void 레슨이_없는_커리큘럼이면_빈_목록을_반환한다() {
		Topic topic = persistTopic("기본 교육");
		Curriculum curriculum = persistCurriculum(topic, "커리큘럼", 1);
		entityManager.flush();

		List<Lesson> lessons = lessonRepository.findAllByCurriculum_IdOrderBySortOrderAscIdAsc(curriculum.getId());

		assertThat(lessons).isEmpty();
	}

	private Topic persistTopic(String title) {
		TrainingCategory category = entityManager.persist(TrainingCategory.create(title + " 카테고리", 1, null, null));
		return entityManager.persist(Topic.create(category, new TopicCreateCommand(title, 1, null, null, null)));
	}

	private Curriculum persistCurriculum(Topic topic, String title, int sortOrder) {
		CurriculumCreateCommand command = new CurriculumCreateCommand(title, sortOrder, null, null);
		return entityManager.persist(Curriculum.create(topic, command));
	}

	private Lesson persistLesson(Curriculum curriculum, String title, int sortOrder) {
		LessonCreateCommand command = new LessonCreateCommand(title, sortOrder, 5);
		return entityManager.persist(Lesson.create(curriculum, command));
	}
}
