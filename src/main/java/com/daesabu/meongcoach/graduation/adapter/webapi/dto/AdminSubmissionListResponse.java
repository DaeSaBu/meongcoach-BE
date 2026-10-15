package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.application.provided.dto.AdminSubmissionResult;
import java.util.List;

public record AdminSubmissionListResponse(List<AdminSubmissionResponse> submissions) {

	public static AdminSubmissionListResponse from(List<AdminSubmissionResult> results) {
		List<AdminSubmissionResponse> submissions = results.stream()
				.map(AdminSubmissionResponse::from)
				.toList();
		return new AdminSubmissionListResponse(submissions);
	}
}
