package com.daesabu.meongcoach.training.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumCreateCommand;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicCreateCommand;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * 커리큘럼 조회 리포지토리 검증.
 */
@DataJpaTest
class CurriculumRepositoryTest {

	@Autowired
	private CurriculumRepository curriculumRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 토픽의_커리큘럼을_정렬_순서_오름차순으로_조회한다() {
		Topic topic = persistTopic("기본 교육");
		persistCurriculum(topic, "셋째", 3);
		persistCurriculum(topic, "첫째", 1);
		persistCurriculum(topic, "둘째", 2);
		entityManager.flush();

		List<Curriculum> curriculums = curriculumRepository.findAllByTopic_IdOrderBySortOrderAscIdAsc(topic.getId());

		assertThat(curriculums).extracting(Curriculum::getTitle)
				.containsExactly("첫째", "둘째", "셋째");
	}

	@Test
	void 정렬_순서가_같으면_id_오름차순으로_조회한다() {
		Topic topic = persistTopic("기본 교육");
		Curriculum first = persistCurriculum(topic, "먼저 등록", 1);
		Curriculum second = persistCurriculum(topic, "나중 등록", 1);
		entityManager.flush();

		List<Curriculum> curriculums = curriculumRepository.findAllByTopic_IdOrderBySortOrderAscIdAsc(topic.getId());

		assertThat(curriculums).extracting(Curriculum::getId)
				.containsExactly(first.getId(), second.getId());
	}

	@Test
	void 다른_토픽의_커리큘럼은_조회되지_않는다() {
		Topic topic = persistTopic("기본 교육");
		Topic otherTopic = persistTopic("문제 행동");
		persistCurriculum(topic, "대상 커리큘럼", 1);
		persistCurriculum(otherTopic, "다른 토픽 커리큘럼", 1);
		entityManager.flush();

		List<Curriculum> curriculums = curriculumRepository.findAllByTopic_IdOrderBySortOrderAscIdAsc(topic.getId());

		assertThat(curriculums).extracting(Curriculum::getTitle)
				.containsExactly("대상 커리큘럼");
	}

	@Test
	void 커리큘럼이_없는_토픽이면_빈_목록을_반환한다() {
		Topic topic = persistTopic("기본 교육");
		entityManager.flush();

		List<Curriculum> curriculums = curriculumRepository.findAllByTopic_IdOrderBySortOrderAscIdAsc(topic.getId());

		assertThat(curriculums).isEmpty();
	}

	private Topic persistTopic(String title) {
		TrainingCategory category = entityManager.persist(TrainingCategory.create(title + " 카테고리", 1, null, null));
		return entityManager.persist(Topic.create(category, new TopicCreateCommand(title, 1, null, null, null)));
	}

	private Curriculum persistCurriculum(Topic topic, String title, int sortOrder) {
		CurriculumCreateCommand command = new CurriculumCreateCommand(title, sortOrder, null, null);
		return entityManager.persist(Curriculum.create(topic, command));
	}
}
