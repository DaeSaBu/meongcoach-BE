package com.daesabu.meongcoach.alert.application;

import com.daesabu.meongcoach.alert.application.provided.Alerter;
import com.daesabu.meongcoach.alert.application.provided.dto.AlertRequest;
import com.daesabu.meongcoach.alert.application.required.AlertSender;
import com.daesabu.meongcoach.alert.domain.Alert;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Validated
public class AlertService implements Alerter {

	private final AlertSender alertSender;

	@Async
	@Override
	public void alert(AlertRequest request) {
		Alert alert = Alert.of(request.toCommand(), Instant.now());
		alertSender.send(alert);
	}
}
