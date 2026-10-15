package com.daesabu.meongcoach.plan.application;

import static com.daesabu.meongcoach.plan.application.CatalogBuilder.catalog;
import static com.daesabu.meongcoach.plan.application.CatalogBuilder.category;
import static com.daesabu.meongcoach.plan.application.CatalogBuilder.curriculum;
import static com.daesabu.meongcoach.plan.application.CatalogBuilder.dog;
import static com.daesabu.meongcoach.plan.application.CatalogBuilder.topic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.plan.application.provided.PlanFinder;
import com.daesabu.meongcoach.plan.application.provided.dto.CurrentPlanResult;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCreateRequest;
import com.daesabu.meongcoach.plan.application.required.PlanGenerator;
import com.daesabu.meongcoach.plan.application.required.TrainingPlanRepository;
import com.daesabu.meongcoach.plan.domain.PlanDraft;
import com.daesabu.meongcoach.plan.domain.PlanDraft.WeekDraft;
import com.daesabu.meongcoach.plan.domain.PlanGeneratorType;
import com.daesabu.meongcoach.progress.application.provided.LessonProgressFinder;
import com.daesabu.meongcoach.training.application.provided.TrainingCatalogFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult;
import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class PlanGenerateServiceBudgetTest {

	private static final Duration CALL_TIMEOUT = Duration.ofSeconds(30);
	private static final Duration TOTAL_BUDGET = Duration.ofSeconds(75);

	@Mock
	private PlanGenerator planGenerator;

	@Mock
	private PlanStoreService planStoreService;

	@Mock
	private PlanFinder planFinder;

	@Mock
	private TrainingPlanRepository trainingPlanRepository;

	@Mock
	private DogProfileFinder dogProfileFinder;

	@Mock
	private TrainingCatalogFinder trainingCatalogFinder;

	@Mock
	private LessonProgressFinder lessonProgressFinder;

	@Mock
	private UserProfileFinder userProfileFinder;

	private AdvancingClock clock;
	private PlanGenerateService service;

	@BeforeEach
	void setUp() {
		clock = new AdvancingClock();
		service = new PlanGenerateService(new PlanValidator(), new FallbackPlanBuilder(), planGenerator,
				planStoreService, planFinder, trainingPlanRepository, dogProfileFinder, trainingCatalogFinder,
				lessonProgressFinder, userProfileFinder, JsonMapper.builder().build(), clock,
				new PlanGenerationProperties(CALL_TIMEOUT, TOTAL_BUDGET));
		TrainingCatalogResult catalog = catalog(category(1, EntitlementType.PUPPY,
				topic(1, "앉아", curriculum(11, "앉아 기초", 1, 2), curriculum(12, "앉아 심화", 2, 2)),
				topic(2, "이리와", curriculum(21, "이리와 기초", 1, 2))));
		given(dogProfileFinder.findProfile(1L, 2L)).willReturn(dog(7, false));
		given(trainingCatalogFinder.findCatalog(1L)).willReturn(catalog);
		given(lessonProgressFinder.findCompletedLessonIds(any(), any())).willReturn(Set.of());
		given(userProfileFinder.findPriorTrainingTopicIds(1L)).willReturn(Set.of());
		given(planFinder.findCurrent(1L)).willReturn(new CurrentPlanResult(false, null));
	}

	@Test
	void 첫_호출이_예산을_거의_쓰면_재시도를_건너뛰고_대체_플랜을_저장한다() {
		given(planGenerator.generate(any(), anyString())).willAnswer(invocation -> {
			clock.advance(Duration.ofSeconds(50));
			return invalidDraft();
		});

		service.create(1L, aiRequest());

		then(planGenerator).should(times(1)).generate(any(), anyString());
		assertThat(storedGeneratorType()).isEqualTo(PlanGeneratorType.FALLBACK);
	}

	@Test
	void 남은_예산이_호출_한도_이상이면_재시도한다() {
		given(planGenerator.generate(any(), anyString())).willAnswer(invocation -> {
			clock.advance(Duration.ofSeconds(45));
			return invalidDraft();
		});

		service.create(1L, aiRequest());

		then(planGenerator).should(times(2)).generate(any(), anyString());
	}

	private PlanGeneratorType storedGeneratorType() {
		ArgumentCaptor<GeneratedPlan> captor = ArgumentCaptor.forClass(GeneratedPlan.class);
		then(planStoreService).should().store(any(), any(), any(), captor.capture(), any());
		return captor.getValue().generatorType();
	}

	private PlanCreateRequest aiRequest() {
		return new PlanCreateRequest(2L, "AI", null, "산책할 때 줄을 당기지 않았으면 좋겠어요", 10, 3, true);
	}

	private PlanDraft invalidDraft() {
		return new PlanDraft("제목", "목표", "이유", List.of(new WeekDraft(1, "테마", "마일스톤", List.of(999L))));
	}

	private static final class AdvancingClock extends Clock {

		private Instant now = Instant.parse("2026-10-15T00:00:00Z");

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneId.of("Asia/Seoul");
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}
	}
}
