package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSummaryResult;
import java.util.List;

public record GraduationListResponse(List<GraduationItemResponse> graduations) {

	public static GraduationListResponse from(List<GraduationSummaryResult> results) {
		return new GraduationListResponse(results.stream().map(GraduationItemResponse::from).toList());
	}
}
