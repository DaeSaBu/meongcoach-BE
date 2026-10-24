package com.daesabu.meongcoach.plan.adapter.webapi;

import com.daesabu.meongcoach.plan.adapter.webapi.dto.CurrentPlanResponse;
import com.daesabu.meongcoach.plan.adapter.webapi.dto.PlanCreateResponse;
import com.daesabu.meongcoach.plan.application.provided.CurrentPlanFinder;
import com.daesabu.meongcoach.plan.application.provided.CustomPlanCreator;
import com.daesabu.meongcoach.plan.application.provided.dto.CurrentPlanResult;
import com.daesabu.meongcoach.plan.application.provided.dto.CustomPlanCreateRequest;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

	private final CustomPlanCreator customPlanCreator;
	private final CurrentPlanFinder currentPlanFinder;

	@GetMapping("/current")
	public CurrentPlanResponse findCurrent(@CurrentUserId Long userId) {
		Optional<CurrentPlanResult> result = currentPlanFinder.find(userId);
		return CurrentPlanResponse.from(result);
	}

	@PostMapping("/custom")
	@ResponseStatus(HttpStatus.CREATED)
	public PlanCreateResponse createCustom(@CurrentUserId Long userId,
	                                      @Valid @RequestBody CustomPlanCreateRequest request) {
		Plan plan = customPlanCreator.create(userId, request);
		return PlanCreateResponse.from(plan);
	}
}
