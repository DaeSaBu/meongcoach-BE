package com.daesabu.meongcoach.onboarding.adapter.webapi;

import com.daesabu.meongcoach.onboarding.adapter.webapi.dto.OnboardingCompleteRequest;
import com.daesabu.meongcoach.onboarding.adapter.webapi.dto.OnboardingCompleteResponse;
import com.daesabu.meongcoach.onboarding.adapter.webapi.dto.OnboardingImageUploadUrlRequest;
import com.daesabu.meongcoach.onboarding.adapter.webapi.dto.OnboardingImageUploadUrlResponse;
import com.daesabu.meongcoach.onboarding.adapter.webapi.dto.OnboardingMetadataResponse;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingCompleter;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlIssuer;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlResult;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingMetadataFinder;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

	private final OnboardingMetadataFinder onboardingMetadataFinder;
	private final OnboardingCompleter onboardingCompleter;
	private final OnboardingImageUploadUrlIssuer onboardingImageUploadUrlIssuer;

	@GetMapping("/metadata")
	public OnboardingMetadataResponse metadata() {
		return OnboardingMetadataResponse.from(onboardingMetadataFinder.find());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OnboardingCompleteResponse complete(@CurrentUserId Long userId,
	                                           @Valid @RequestBody OnboardingCompleteRequest request) {
		List<Long> dogIds = onboardingCompleter.complete(userId, request.toInfo());
		return new OnboardingCompleteResponse(dogIds);
	}

	@PostMapping("/presigned-urls")
	public OnboardingImageUploadUrlResponse issueImageUploadUrl(@CurrentUserId Long userId,
	                                                            @Valid @RequestBody OnboardingImageUploadUrlRequest request) {
		OnboardingImageUploadUrlResult result =
				onboardingImageUploadUrlIssuer.issue(userId, request.target(), request.contentType());
		return OnboardingImageUploadUrlResponse.from(result);
	}
}
