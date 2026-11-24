package com.daesabu.meongcoach.report.application;

import com.daesabu.meongcoach.alert.application.provided.Alerter;
import com.daesabu.meongcoach.alert.application.provided.dto.AlertRequest;
import com.daesabu.meongcoach.alert.domain.shared.AlertField;
import com.daesabu.meongcoach.report.domain.ReportRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ReportAlertListener {

	private final Alerter alerter;

	@TransactionalEventListener
	public void alert(ReportRegisteredEvent event) {
		alerter.alert(AlertRequest.of("신고 접수",
				AlertField.of("reportId", event.reportId()),
				AlertField.of("targetType", event.targetType()),
				AlertField.of("targetId", event.targetId()),
				AlertField.of("reason", event.reason()),
				AlertField.of("reporterId", event.reporterId())));
	}
}
