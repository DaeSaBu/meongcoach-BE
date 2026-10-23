package com.daesabu.meongcoach.plan.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.dto.DogSummaryResult;
import com.daesabu.meongcoach.plan.application.provided.dto.CustomPlanCreateRequest;
import com.daesabu.meongcoach.plan.application.required.PlanRepository;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.plan.domain.exception.EmptyPlanCurriculumsException;
import com.daesabu.meongcoach.plan.domain.exception.PlanCreationConflictException;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import com.daesabu.meongcoach.training.application.provided.CustomPlanPreparationFinder;
import com.daesabu.meongcoach.training.application.provided.TopicCompletionChecker;
import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.dto.CustomPlanPreparationResult;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@NonTransactionalApplicationTest
class CustomPlanCreatorTest {

	private static final Long USER_ID = 597001L;
	private static final CustomPlanCreateRequest REQUEST = new CustomPlanCreateRequest(List.of(11L), 10, 3);

	@Autowired
	private CustomPlanCreator creator;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private EntityManager entityManager;
	@MockitoBean
	private DogProfileFinder dogProfileFinder;
	@MockitoBean(extraInterfaces = {TopicFinder.class, TopicCompletionChecker.class})
	private CustomPlanPreparationFinder preparationFinder;
	@MockitoSpyBean
	private PlanRepository planRepository;

	@BeforeEach
	void 준비한다() {
		jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_plans_active_user ON plans (user_id) WHERE status = 'ACTIVE'");
		when(dogProfileFinder.findSelectedDogSummary(USER_ID)).thenReturn(new DogSummaryResult(7L, "초코"));
		when(preparationFinder.find(USER_ID, REQUEST.topicIds())).thenReturn(
				new CustomPlanPreparationResult(List.of(9L, 4L)));
	}

	@AfterEach
	void 정리한다() {
		jdbc.update("DELETE FROM plan_curriculums WHERE plan_id IN (SELECT id FROM plans WHERE user_id = ?)", USER_ID);
		jdbc.update("DELETE FROM plans WHERE user_id = ?", USER_ID);
	}

	@Test
	void 선택된_강아지와_교육_순서로_플랜을_저장하고_기존_플랜을_교체한다() {
		Plan first = creator.create(USER_ID, REQUEST);

		Plan second = creator.create(USER_ID, REQUEST);

		assertThat(second.getDogId()).isEqualTo(7L);
		assertThat(second.getTitle()).isEqualTo("초코의 2주 훈련 플랜");
		assertThat(second.getGoal()).isEqualTo("선택한 훈련을 차근차근 익혀요");
		assertThat(second.getCurriculumIds()).containsExactly(9L, 4L);
		assertThat(planRepository.findById(first.getId()).orElseThrow().getStatus()).isEqualTo(PlanStatus.REPLACED);
		assertThat(planRepository.findByUserIdAndStatus(USER_ID, PlanStatus.ACTIVE).orElseThrow().getId())
				.isEqualTo(second.getId());
	}

	@Test
	void 남은_커리큘럼이_없으면_기존_활성_플랜을_유지한다() {
		Plan first = creator.create(USER_ID, REQUEST);
		when(preparationFinder.find(USER_ID, REQUEST.topicIds())).thenReturn(
				new CustomPlanPreparationResult(List.of()));

		assertThatThrownBy(() -> creator.create(USER_ID, REQUEST)).isInstanceOf(EmptyPlanCurriculumsException.class);

		assertThat(planRepository.findByUserIdAndStatus(USER_ID, PlanStatus.ACTIVE).orElseThrow().getId())
				.isEqualTo(first.getId());
	}

	@Test
	void 무결성_오류가_발생하면_교체를_롤백하고_원인을_보존한_충돌로_알린다() {
		Plan first = creator.create(USER_ID, REQUEST);
		DataIntegrityViolationException failure = new DataIntegrityViolationException("다른 저장 오류");
		doThrow(failure).when(planRepository).saveAndFlush(any(Plan.class));

		assertThatThrownBy(() -> creator.create(USER_ID, REQUEST))
				.isInstanceOf(PlanCreationConflictException.class)
				.hasCause(failure);

		assertThat(planRepository.findById(first.getId()).orElseThrow().getStatus()).isEqualTo(PlanStatus.ACTIVE);
	}

	@Test
	void 동시에_처음_생성하면_하나만_저장하고_나머지는_충돌로_알린다() throws Exception {
		verifyConcurrentCreation();
	}

	@Test
	void 동시에_교체하면_하나만_저장하고_실패한_교체는_롤백한다() throws Exception {
		Plan first = creator.create(USER_ID, REQUEST);

		verifyConcurrentCreation();

		assertThat(planRepository.findById(first.getId()).orElseThrow().getStatus()).isEqualTo(PlanStatus.REPLACED);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM plans WHERE user_id = ?", Long.class, USER_ID))
				.isEqualTo(2L);
	}

	private void verifyConcurrentCreation() throws Exception {
		CyclicBarrier barrier = new CyclicBarrier(2);
		doAnswer(invocation -> {
			var result = entityManager.createQuery(
					"select p from Plan p where p.userId = :userId and p.status = :status", Plan.class)
					.setParameter("userId", USER_ID).setParameter("status", PlanStatus.ACTIVE)
					.getResultList().stream().findFirst();
			barrier.await(10, TimeUnit.SECONDS);
			return result;
		}).when(planRepository).findByUserIdAndStatus(USER_ID, PlanStatus.ACTIVE);

		try (var executor = Executors.newFixedThreadPool(2)) {
			var first = executor.submit(this::createOutcome);
			var second = executor.submit(this::createOutcome);
			assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
					.containsExactlyInAnyOrder("created", "conflict");
		}
		assertThat(jdbc.queryForObject("SELECT count(*) FROM plans WHERE user_id = ? AND status = 'ACTIVE'",
				Long.class, USER_ID)).isEqualTo(1L);
	}

	private String createOutcome() {
		try {
			creator.create(USER_ID, REQUEST);
			return "created";
		} catch (PlanCreationConflictException exception) {
			return "conflict";
		}
	}
}
