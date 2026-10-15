package com.daesabu.meongcoach.graduation.adapter.webapi;

import com.daesabu.meongcoach.graduation.adapter.webapi.dto.GraduationListResponse;
import com.daesabu.meongcoach.graduation.adapter.webapi.dto.GraduationResponse;
import com.daesabu.meongcoach.graduation.adapter.webapi.dto.GraduationRewardResponse;
import com.daesabu.meongcoach.graduation.adapter.webapi.dto.GraduationSubmissionResponse;
import com.daesabu.meongcoach.graduation.adapter.webapi.dto.GraduationUploadUrlResponse;
import com.daesabu.meongcoach.graduation.application.provided.GraduationFinder;
import com.daesabu.meongcoach.graduation.application.provided.GraduationRewardClaimer;
import com.daesabu.meongcoach.graduation.application.provided.GraduationSubmitter;
import com.daesabu.meongcoach.graduation.application.provided.GraduationUploadUrlIssuer;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationUploadUrlRequest;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
public class GraduationController {

	private static final String SUBMISSION_LOCATION = "/api/training/topics/{topicId}/graduation/submissions/{submissionId}";

	private final GraduationFinder graduationFinder;
	private final GraduationUploadUrlIssuer graduationUploadUrlIssuer;
	private final GraduationSubmitter graduationSubmitter;
	private final GraduationRewardClaimer graduationRewardClaimer;

	@GetMapping("/graduations")
	public GraduationListResponse findGraduations(@CurrentUserId Long userId) {
		return GraduationListResponse.from(graduationFinder.findGraduations(userId));
	}

	@GetMapping("/topics/{topicId}/graduation")
	public GraduationResponse findGraduation(@CurrentUserId Long userId, @PathVariable Long topicId) {
		return GraduationResponse.from(graduationFinder.findGraduation(userId, topicId));
	}

	@PostMapping("/topics/{topicId}/graduation/presigned-urls")
	@ResponseStatus(HttpStatus.CREATED)
	public GraduationUploadUrlResponse issueUploadUrl(@CurrentUserId Long userId, @PathVariable Long topicId,
	                                                  @Valid @RequestBody GraduationUploadUrlRequest request) {
		return GraduationUploadUrlResponse.from(graduationUploadUrlIssuer.issue(userId, topicId, request));
	}

	@PostMapping("/topics/{topicId}/graduation/submissions")
	public ResponseEntity<GraduationSubmissionResponse> submit(@CurrentUserId Long userId, @PathVariable Long topicId,
	                                                           @Valid @RequestBody GraduationSubmissionRequest request) {
		GraduationSubmission submission = graduationSubmitter.submit(userId, topicId, request);
		URI location = UriComponentsBuilder.fromPath(SUBMISSION_LOCATION).build(topicId, submission.getId());
		return ResponseEntity.created(location).body(GraduationSubmissionResponse.from(submission));
	}

	@PutMapping("/topics/{topicId}/graduation/reward")
	public GraduationRewardResponse claimReward(@CurrentUserId Long userId, @PathVariable Long topicId) {
		return GraduationRewardResponse.from(graduationRewardClaimer.claim(userId, topicId));
	}
}
