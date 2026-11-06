package com.daesabu.meongcoach.plan.adapter.webapi;

import com.daesabu.meongcoach.plan.adapter.webapi.dto.CurrentPlanResponse;
import com.daesabu.meongcoach.plan.adapter.webapi.dto.PlanCreateResponse;
import com.daesabu.meongcoach.plan.adapter.webapi.dto.PlanOutcomeResponse;
import com.daesabu.meongcoach.plan.application.provided.CurrentPlanFinder;
import com.daesabu.meongcoach.plan.application.provided.PlanLessonCompleter;
import com.daesabu.meongcoach.plan.application.provided.PlanRegister;
import com.daesabu.meongcoach.plan.application.provided.dto.AiPlanCreateRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.CurrentPlanResult;
import com.daesabu.meongcoach.plan.application.provided.dto.CustomPlanCreateRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanOutcomeRecordRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanOutcomeResult;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

	private final PlanRegister planRegister;
	private final CurrentPlanFinder currentPlanFinder;
	private final PlanLessonCompleter planLessonCompleter;

	@PostMapping("/{planId}/lessons/{lessonId}/completion")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void completeLesson(@CurrentUserId Long userId, @PathVariable Long planId, @PathVariable Long lessonId) {
		planLessonCompleter.complete(userId, planId, lessonId);
	}

	@PostMapping("/{planId}/outcome")
	public PlanOutcomeResponse recordOutcome(@CurrentUserId Long userId, @PathVariable Long planId,
			@Valid @RequestBody PlanOutcomeRecordRequest request) {
		PlanOutcomeResult result = planRegister.recordOutcome(userId, planId, request);
		return PlanOutcomeResponse.from(result);
	}

	/**
	 * @deprecated 사용 중단 예정. GET /api/plans/current?dogId={dogId}로 이전한다.
	 */
	@Deprecated(forRemoval = true)
	@GetMapping(path = "/current", params = "!dogId")
	public CurrentPlanResponse findCurrent(@CurrentUserId Long userId) {
		Optional<CurrentPlanResult> result = currentPlanFinder.find(userId);
		return CurrentPlanResponse.from(result);
	}

	@GetMapping(path = "/current", params = "dogId")
	public CurrentPlanResponse findCurrent(@CurrentUserId Long userId, @RequestParam Long dogId) {
		Optional<CurrentPlanResult> result = currentPlanFinder.find(userId, dogId);
		return CurrentPlanResponse.from(result);
	}

	/**
	 * @deprecated 사용 중단 예정. POST /api/plans/custom?dogId={dogId}로 이전한다.
	 */
	@Deprecated(forRemoval = true)
	@PostMapping(path = "/custom", params = "!dogId")
	@ResponseStatus(HttpStatus.CREATED)
	public PlanCreateResponse createCustom(@CurrentUserId Long userId,
	                                      @Valid @RequestBody CustomPlanCreateRequest request) {
		Plan plan = planRegister.createCustom(userId, request);
		return PlanCreateResponse.from(plan);
	}

	@PostMapping(path = "/custom", params = "dogId")
	@ResponseStatus(HttpStatus.CREATED)
	public PlanCreateResponse createCustom(@CurrentUserId Long userId, @RequestParam Long dogId,
			@Valid @RequestBody CustomPlanCreateRequest request) {
		Plan plan = planRegister.createCustom(userId, dogId, request);
		return PlanCreateResponse.from(plan);
	}

	/**
	 * @deprecated 사용 중단 예정. POST /api/plans/ai?dogId={dogId}로 이전한다.
	 */
	@Deprecated(forRemoval = true)
	@PostMapping(path = "/ai", params = "!dogId")
	@ResponseStatus(HttpStatus.CREATED)
	public PlanCreateResponse createAiPlan(@CurrentUserId Long userId,
			@Valid @RequestBody AiPlanCreateRequest request) {
		Plan plan = planRegister.createAiPlan(userId, request);
		return PlanCreateResponse.from(plan);
	}

	@PostMapping(path = "/ai", params = "dogId")
	@ResponseStatus(HttpStatus.CREATED)
	public PlanCreateResponse createAiPlan(@CurrentUserId Long userId, @RequestParam Long dogId,
			@Valid @RequestBody AiPlanCreateRequest request) {
		Plan plan = planRegister.createAiPlan(userId, dogId, request);
		return PlanCreateResponse.from(plan);
	}
}
