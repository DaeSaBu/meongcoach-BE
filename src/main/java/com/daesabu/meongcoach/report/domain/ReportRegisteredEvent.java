package com.daesabu.meongcoach.report.domain;

import com.daesabu.meongcoach.report.domain.shared.ReportTargetType;

public record ReportRegisteredEvent(
		Long reportId,
		Long reporterId,
		ReportTargetType targetType,
		Long targetId,
		ReportReason reason
) {

	public static ReportRegisteredEvent from(Report report) {
		return new ReportRegisteredEvent(report.getId(), report.getReporterId(), report.getTargetType(),
				report.getTargetId(), report.getReason());
	}
}
