package com.daesabu.meongcoach.plan.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class PlanPersistenceTest {

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 플랜만_저장해도_커리큘럼_ID와_순서가_보존된다() {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "제목", "목표", 10, 3);
		Plan plan = Plan.create(command, List.of(7L, 4L, 12L));
		entityManager.persistAndFlush(plan);
		Long planId = plan.getId();
		entityManager.clear();

		Plan found = entityManager.find(Plan.class, planId);

		assertThat(found.getCurriculumIds()).containsExactly(7L, 4L, 12L);
	}

	@Test
	void 같은_커리큘럼을_포함한_플랜들의_목록은_서로_섞이지_않는다() {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "제목", "목표", 10, 3);
		Plan first = Plan.create(command, List.of(7L, 4L));
		Plan second = Plan.create(command, List.of(4L, 12L));
		entityManager.persist(first);
		entityManager.persistAndFlush(second);
		Long firstId = first.getId();
		Long secondId = second.getId();
		entityManager.clear();

		Plan foundFirst = entityManager.find(Plan.class, firstId);
		Plan foundSecond = entityManager.find(Plan.class, secondId);

		assertThat(foundFirst.getCurriculumIds()).containsExactly(7L, 4L);
		assertThat(foundSecond.getCurriculumIds()).containsExactly(4L, 12L);
	}
}
