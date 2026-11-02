package com.daesabu.meongcoach.alert.application;

import com.daesabu.meongcoach.alert.application.required.AlertSender;
import com.daesabu.meongcoach.alert.domain.Alert;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlertDeliveryService {

	private final AlertSender alertSender;

	@Async
	public void deliver(Alert alert) {
		alertSender.send(alert);
	}
}
