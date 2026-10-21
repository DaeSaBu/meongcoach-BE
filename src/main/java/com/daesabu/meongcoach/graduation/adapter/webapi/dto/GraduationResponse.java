package com.daesabu.meongcoach.graduation.adapter.webapi.dto;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationDetailResult;

public record GraduationResponse(Long topicId, String topicTitle, boolean eligible, TopicGraduationResponse graduation) {

	public static GraduationResponse from(GraduationDetailResult result) {
		if (result.submission() == null) {
			return new GraduationResponse(result.topic().id(), result.topic().title(), result.eligible(), null);
		}
		TopicGraduationResponse graduation = TopicGraduationResponse.of(
				result.submission(), result.videoUrl());
		return new GraduationResponse(result.topic().id(), result.topic().title(), result.eligible(), graduation);
	}
}
