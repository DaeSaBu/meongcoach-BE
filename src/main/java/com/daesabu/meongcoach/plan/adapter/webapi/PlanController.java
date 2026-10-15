package com.daesabu.meongcoach.plan.adapter.webapi;

import com.daesabu.meongcoach.plan.adapter.webapi.dto.CurrentPlanResponse;
import com.daesabu.meongcoach.plan.adapter.webapi.dto.PlanCompletionResponse;
import com.daesabu.meongcoach.plan.adapter.webapi.dto.PlanDetailResponse;
import com.daesabu.meongcoach.plan.application.provided.PlanCompleter;
import com.daesabu.meongcoach.plan.application.provided.PlanCreator;
import com.daesabu.meongcoach.plan.application.provided.PlanFinder;
import com.daesabu.meongcoach.plan.application.provided.dto.CurrentPlanResult;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCompleteRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCompletionResult;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCreateRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

	private final PlanCreator planCreator;
	private final PlanFinder planFinder;
	private final PlanCompleter planCompleter;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PlanDetailResponse createPlan(@CurrentUserId Long userId, @Valid @RequestBody PlanCreateRequest request) {
		PlanDetailResult result = planCreator.create(userId, request);
		return PlanDetailResponse.from(result);
	}

	@GetMapping("/current")
	public CurrentPlanResponse findCurrentPlan(@CurrentUserId Long userId) {
		CurrentPlanResult result = planFinder.findCurrent(userId);
		return CurrentPlanResponse.from(result);
	}

	@PostMapping("/{planId}/completion")
	@ResponseStatus(HttpStatus.CREATED)
	public PlanCompletionResponse completePlan(@CurrentUserId Long userId, @PathVariable Long planId,
			@Valid @RequestBody PlanCompleteRequest request) {
		PlanCompletionResult result = planCompleter.complete(userId, planId, request);
		return PlanCompletionResponse.from(result);
	}
}
