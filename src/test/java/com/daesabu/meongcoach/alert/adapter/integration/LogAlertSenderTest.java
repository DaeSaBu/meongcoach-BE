package com.daesabu.meongcoach.alert.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.alert.domain.Alert;
import com.daesabu.meongcoach.alert.domain.shared.AlertField;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class LogAlertSenderTest {

	@Test
	void 알림을_info_로그로_남긴다(CapturedOutput output) {
		LogAlertSender sender = new LogAlertSender();
		Alert alert = new Alert("영상 분석 지연",
				List.of(new AlertField("userId", "42")), Instant.parse("2026-11-02T12:34:56Z"));

		sender.send(alert);

		assertThat(output).contains("INFO", "영상 분석 지연", "userId", "42");
	}
}
