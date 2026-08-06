package com.daesabu.meongcoach.ai.adapter.webapi.dto;

import com.daesabu.meongcoach.ai.application.provided.AiTrialView;

public record AiTrialResponse(int usedCount, int maxCount, int remainingCount) {

	public static AiTrialResponse from(AiTrialView view) {
		return new AiTrialResponse(view.usedCount(), view.maxCount(), view.remainingCount());
	}
}
