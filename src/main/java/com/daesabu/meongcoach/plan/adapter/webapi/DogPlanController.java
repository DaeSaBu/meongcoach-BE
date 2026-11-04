package com.daesabu.meongcoach.plan.adapter.webapi;

import com.daesabu.meongcoach.plan.adapter.webapi.dto.PlanCreateResponse;
import com.daesabu.meongcoach.plan.application.provided.PlanRegister;
import com.daesabu.meongcoach.plan.application.provided.dto.AiPlanCreateRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.CustomPlanCreateRequest;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dogs/{dogId}/plans")
@RequiredArgsConstructor
public class DogPlanController {

	private final PlanRegister planRegister;

	@PostMapping("/custom")
	@ResponseStatus(HttpStatus.CREATED)
	public PlanCreateResponse createCustom(@CurrentUserId Long userId, @PathVariable Long dogId,
			@Valid @RequestBody CustomPlanCreateRequest request) {
		Plan plan = planRegister.createCustom(userId, dogId, request);
		return PlanCreateResponse.from(plan);
	}

	@PostMapping("/ai")
	@ResponseStatus(HttpStatus.CREATED)
	public PlanCreateResponse createAiPlan(@CurrentUserId Long userId, @PathVariable Long dogId,
			@Valid @RequestBody AiPlanCreateRequest request) {
		Plan plan = planRegister.createAiPlan(userId, dogId, request);
		return PlanCreateResponse.from(plan);
	}
}
