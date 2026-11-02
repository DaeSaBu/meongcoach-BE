package com.daesabu.meongcoach.alert.adapter.integration;

import com.daesabu.meongcoach.alert.application.required.AlertSender;
import com.daesabu.meongcoach.alert.domain.Alert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "meongcoach.alert.slack", name = "enabled", havingValue = "false")
public class LogAlertSender implements AlertSender {

	@Override
	public void send(Alert alert) {
		log.info("개발자 알림: level={}, title={}, fields={}", alert.level(), alert.title(), alert.fields());
	}
}
