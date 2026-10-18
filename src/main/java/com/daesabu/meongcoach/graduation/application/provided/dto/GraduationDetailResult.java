package com.daesabu.meongcoach.graduation.application.provided.dto;

import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;

public record GraduationDetailResult(TopicResult topic, boolean eligible, TopicGraduation graduation, String videoUrl) {

	public static GraduationDetailResult notSubmitted(TopicResult topic, boolean eligible) {
		return new GraduationDetailResult(topic, eligible, null, null);
	}
}
