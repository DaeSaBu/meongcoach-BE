package com.daesabu.meongcoach.analysis.adapter.webapi.dto;

import com.daesabu.meongcoach.analysis.domain.AiTrial;

public record AiTrialResponse(int usedCount, int maxCount, int remainingCount) {

	public static AiTrialResponse from(AiTrial aiTrial) {
		return new AiTrialResponse(aiTrial.getUsedCount(), aiTrial.maxCount(), aiTrial.remainingCount());
	}
}
