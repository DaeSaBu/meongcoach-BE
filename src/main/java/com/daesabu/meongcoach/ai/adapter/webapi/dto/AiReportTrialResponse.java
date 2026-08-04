package com.daesabu.meongcoach.ai.adapter.webapi.dto;

import com.daesabu.meongcoach.ai.application.provided.AiReportTrialView;

public record AiReportTrialResponse(int usedCount, int maxCount, int remainingCount) {

	public static AiReportTrialResponse from(AiReportTrialView view) {
		return new AiReportTrialResponse(view.usedCount(), view.maxCount(), view.remainingCount());
	}
}
