package com.daesabu.meongcoach.graduation.adapter.webapi;

import com.daesabu.meongcoach.graduation.adapter.webapi.dto.AdminReviewResponse;
import com.daesabu.meongcoach.graduation.application.provided.GraduationReviewer;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationReviewRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/graduations")
@RequiredArgsConstructor
public class GraduationAdminController {

	private final GraduationReviewer graduationReviewer;

	@PutMapping("/{graduationId}/review")
	public AdminReviewResponse review(@PathVariable Long graduationId, @Valid @RequestBody GraduationReviewRequest request) {
		return AdminReviewResponse.from(graduationReviewer.review(graduationId, request));
	}
}
