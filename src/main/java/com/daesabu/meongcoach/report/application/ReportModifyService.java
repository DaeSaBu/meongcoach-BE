package com.daesabu.meongcoach.report.application;

import com.daesabu.meongcoach.report.application.provided.ReportRegister;
import com.daesabu.meongcoach.report.application.provided.dto.ReportRegisterRequest;
import com.daesabu.meongcoach.report.application.required.ReportRepository;
import com.daesabu.meongcoach.report.domain.Report;
import com.daesabu.meongcoach.report.domain.ReportRegisterCommand;
import com.daesabu.meongcoach.report.domain.exception.ReportAlreadyRegisteredException;
import com.daesabu.meongcoach.report.domain.shared.ReportTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class ReportModifyService implements ReportRegister {

	private final ReportRepository reportRepository;

	@Override
	@Transactional
	public void register(Long reporterId, ReportTargetType targetType, Long targetId, ReportRegisterRequest request) {
		ReportRegisterCommand command = request.toCommand(reporterId, targetType, targetId);
		reportRepository.lockByReporterId(reporterId);
		if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, targetType, targetId)) {
			throw new ReportAlreadyRegisteredException();
		}

		reportRepository.save(Report.register(command));
	}
}
