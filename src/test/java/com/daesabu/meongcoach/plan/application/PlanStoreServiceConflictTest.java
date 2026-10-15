package com.daesabu.meongcoach.plan.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.DogProfileUpdater;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCreateRequest;
import com.daesabu.meongcoach.plan.application.required.TrainingPlanRepository;
import com.daesabu.meongcoach.plan.domain.PlanDraft;
import com.daesabu.meongcoach.plan.domain.PlanDraft.WeekDraft;
import com.daesabu.meongcoach.plan.domain.PlanGeneratorType;
import com.daesabu.meongcoach.plan.domain.PlanMode;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.plan.domain.exception.PlanAlreadyActiveException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class PlanStoreServiceConflictTest {

	@Mock
	private TrainingPlanRepository trainingPlanRepository;

	@Mock
	private DogProfileFinder dogProfileFinder;

	@Mock
	private DogProfileUpdater dogProfileUpdater;

	@Test
	void 부분_유니크_인덱스를_위반하면_이미_진행_중인_플랜_충돌로_번역한다() {
		Clock clock = Clock.fixed(Instant.parse("2026-10-15T00:00:00Z"), ZoneId.of("Asia/Seoul"));
		PlanStoreService service = new PlanStoreService(trainingPlanRepository, dogProfileFinder, dogProfileUpdater,
				clock);
		given(trainingPlanRepository.findByUserIdAndStatus(1L, PlanStatus.ACTIVE)).willReturn(List.of());
		given(trainingPlanRepository.saveAndFlush(any())).willThrow(new DataIntegrityViolationException("중복"));
		PlanDraft draft = new PlanDraft("제목", "목표", "이유", List.of(new WeekDraft(1, "테마", "마일스톤", List.of(1L))));
		PlanCreateRequest request = new PlanCreateRequest(2L, "MANUAL", List.of(1L), null, 10, 3, false);
		GeneratedPlan result = new GeneratedPlan(draft, 1, null, PlanGeneratorType.MANUAL);

		assertThatThrownBy(() -> service.store(1L, PlanMode.MANUAL, request, result, "{}"))
				.isInstanceOf(PlanAlreadyActiveException.class);
	}
}
