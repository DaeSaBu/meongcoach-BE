package com.daesabu.meongcoach.plan.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanCreateCommand;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class PlanRepositoryTest {

	@Autowired
	private PlanRepository planRepository;

	@Test
	void 플랜_ID와_소유자가_모두_일치할_때만_조회한다() {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "제목", "목표", 10, 3);
		Plan plan = Plan.create(command, List.of(7L));
		planRepository.saveAndFlush(plan);

		assertThat(planRepository.findByIdAndUserId(plan.getId(), 1L)).contains(plan);
		assertThat(planRepository.findByIdAndUserId(plan.getId(), 3L)).isEmpty();
		assertThat(planRepository.findByIdAndUserId(-1L, 1L)).isEmpty();
	}
}
