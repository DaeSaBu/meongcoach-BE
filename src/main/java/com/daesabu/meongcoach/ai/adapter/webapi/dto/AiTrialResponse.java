package com.daesabu.meongcoach.ai.adapter.webapi.dto;

import com.daesabu.meongcoach.ai.domain.vo.AiTrial;

public record AiTrialResponse(int usedCount, int maxCount, int remainingCount) {

	public static AiTrialResponse from(AiTrial aiTrial) {
		return new AiTrialResponse(aiTrial.usedCount(), aiTrial.maxCount(), aiTrial.remainingCount());
	}
}
