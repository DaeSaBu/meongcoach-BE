package com.daesabu.meongcoach.report.application.provided;

import com.daesabu.meongcoach.report.application.provided.dto.ReportRegisterRequest;
import com.daesabu.meongcoach.report.domain.shared.ReportTargetType;
import jakarta.validation.Valid;

/**
 * 신고 변경 능력.
 */
public interface ReportRegister {

	/**
	 * 회원이 대상을 신고한 기록을 저장한다. 대상이 실제로 있는지는 확인하지 않는다. 사유가 지원하지 않는 값이면 {@code InvalidReportReasonException}, 같은 회원이 같은 대상을
	 * 이미 신고했으면 {@code ReportAlreadyRegisteredException}을 던진다.
	 */
	void register(Long reporterId, ReportTargetType targetType, Long targetId, @Valid ReportRegisterRequest request);
}
