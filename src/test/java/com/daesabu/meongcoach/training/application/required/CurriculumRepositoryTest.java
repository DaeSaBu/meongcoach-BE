package com.daesabu.meongcoach.training.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumCreateCommand;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicCreateCommand;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * 커리큘럼 조회 리포지토리 검증.
 */
@DataJpaTest
@DisplayName("커리큘럼 리포지토리")
class CurriculumRepositoryTest {

	@Autowired
	private CurriculumRepository curriculumRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("토픽의 커리큘럼을 정렬 순서 오름차순으로 조회한다")
	void findAllByTopicOrdersBySortOrderAscending() {
		Topic topic = persistTopic("기본 훈련");
		persistCurriculum(topic, "셋째", 3);
		persistCurriculum(topic, "첫째", 1);
		persistCurriculum(topic, "둘째", 2);
		entityManager.flush();

		List<Curriculum> curriculums = curriculumRepository.findAllByTopic_IdOrderBySortOrderAscIdAsc(topic.getId());

		assertThat(curriculums).extracting(Curriculum::getTitle)
				.containsExactly("첫째", "둘째", "셋째");
	}

	@Test
	@DisplayName("정렬 순서가 같으면 id 오름차순으로 조회한다")
	void findAllByTopicOrdersByIdAscendingWhenSortOrderIsSame() {
		Topic topic = persistTopic("기본 훈련");
		Curriculum first = persistCurriculum(topic, "먼저 등록", 1);
		Curriculum second = persistCurriculum(topic, "나중 등록", 1);
		entityManager.flush();

		List<Curriculum> curriculums = curriculumRepository.findAllByTopic_IdOrderBySortOrderAscIdAsc(topic.getId());

		assertThat(curriculums).extracting(Curriculum::getId)
				.containsExactly(first.getId(), second.getId());
	}

	@Test
	@DisplayName("다른 토픽의 커리큘럼은 조회되지 않는다")
	void findAllByTopicExcludesOtherTopicCurriculums() {
		Topic topic = persistTopic("기본 훈련");
		Topic otherTopic = persistTopic("문제 행동");
		persistCurriculum(topic, "대상 커리큘럼", 1);
		persistCurriculum(otherTopic, "다른 토픽 커리큘럼", 1);
		entityManager.flush();

		List<Curriculum> curriculums = curriculumRepository.findAllByTopic_IdOrderBySortOrderAscIdAsc(topic.getId());

		assertThat(curriculums).extracting(Curriculum::getTitle)
				.containsExactly("대상 커리큘럼");
	}

	@Test
	@DisplayName("커리큘럼이 없는 토픽이면 빈 목록을 반환한다")
	void findAllByTopicReturnsEmptyListWhenTopicHasNoCurriculum() {
		Topic topic = persistTopic("기본 훈련");
		entityManager.flush();

		List<Curriculum> curriculums = curriculumRepository.findAllByTopic_IdOrderBySortOrderAscIdAsc(topic.getId());

		assertThat(curriculums).isEmpty();
	}

	private Topic persistTopic(String title) {
		TrainingCategory category = entityManager.persist(TrainingCategory.create(title + " 카테고리", 1));
		return entityManager.persist(Topic.create(category, new TopicCreateCommand(title, 1)));
	}

	private Curriculum persistCurriculum(Topic topic, String title, int sortOrder) {
		CurriculumCreateCommand command = new CurriculumCreateCommand(title, sortOrder, null, null);
		return entityManager.persist(Curriculum.create(topic, command));
	}
}
