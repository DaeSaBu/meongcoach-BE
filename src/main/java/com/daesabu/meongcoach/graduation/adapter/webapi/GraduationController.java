package com.daesabu.meongcoach.graduation.adapter.webapi;

import com.daesabu.meongcoach.graduation.adapter.webapi.dto.GraduationListResponse;
import com.daesabu.meongcoach.graduation.adapter.webapi.dto.GraduationResponse;
import com.daesabu.meongcoach.graduation.adapter.webapi.dto.GraduationSubmissionResponse;
import com.daesabu.meongcoach.graduation.adapter.webapi.dto.GraduationUploadUrlResponse;
import com.daesabu.meongcoach.graduation.application.provided.GraduationFinder;
import com.daesabu.meongcoach.graduation.application.provided.GraduationSubmitter;
import com.daesabu.meongcoach.graduation.application.provided.GraduationUploadUrlIssuer;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationUploadUrlRequest;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
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
@RequestMapping("/api/training")
@RequiredArgsConstructor
public class GraduationController {

	private final GraduationFinder graduationFinder;
	private final GraduationUploadUrlIssuer graduationUploadUrlIssuer;
	private final GraduationSubmitter graduationSubmitter;

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
	@ResponseStatus(HttpStatus.CREATED)
	public GraduationSubmissionResponse submit(@CurrentUserId Long userId, @PathVariable Long topicId,
	                                           @Valid @RequestBody GraduationSubmissionRequest request) {
		TopicGraduation graduation = graduationSubmitter.submit(userId, topicId, request);
		return GraduationSubmissionResponse.from(graduation);
	}
}
