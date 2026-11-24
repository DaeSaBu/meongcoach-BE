package com.daesabu.meongcoach.report.domain;

import com.daesabu.meongcoach.report.domain.shared.ReportTargetType;

public record ReportRegisterCommand(
		Long reporterId,
		ReportTargetType targetType,
		Long targetId,
		ReportReason reason,
		String detail
) {
}
