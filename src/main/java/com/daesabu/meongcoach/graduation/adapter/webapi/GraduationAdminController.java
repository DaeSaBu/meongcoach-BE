package com.daesabu.meongcoach.graduation.adapter.webapi;

import com.daesabu.meongcoach.graduation.adapter.webapi.dto.AdminReviewResponse;
import com.daesabu.meongcoach.graduation.adapter.webapi.dto.AdminSubmissionListResponse;
import com.daesabu.meongcoach.graduation.application.provided.GraduationReviewer;
import com.daesabu.meongcoach.graduation.application.provided.GraduationSubmissionFinder;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationReviewRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/graduations")
@RequiredArgsConstructor
public class GraduationAdminController {

	private final GraduationSubmissionFinder graduationSubmissionFinder;
	private final GraduationReviewer graduationReviewer;

	@GetMapping("/submissions")
	public AdminSubmissionListResponse findSubmissions(@RequestParam(defaultValue = "PENDING") String result) {
		return AdminSubmissionListResponse.from(graduationSubmissionFinder.findSubmissions(result));
	}

	@PutMapping("/submissions/{submissionId}/review")
	public AdminReviewResponse review(@PathVariable Long submissionId, @Valid @RequestBody GraduationReviewRequest request) {
		return AdminReviewResponse.from(graduationReviewer.review(submissionId, request));
	}
}
