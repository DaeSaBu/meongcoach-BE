package com.daesabu.meongcoach.report.application.provided.dto;

import com.daesabu.meongcoach.report.domain.ReportReason;
import com.daesabu.meongcoach.report.domain.ReportRegisterCommand;
import com.daesabu.meongcoach.report.domain.shared.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportRegisterRequest(
		@NotBlank String reason,
		@Size(max = 500) String detail
) {

	public ReportRegisterCommand toCommand(Long reporterId, ReportTargetType targetType, Long targetId) {
		return new ReportRegisterCommand(reporterId, targetType, targetId, ReportReason.from(reason), detail);
	}
}
