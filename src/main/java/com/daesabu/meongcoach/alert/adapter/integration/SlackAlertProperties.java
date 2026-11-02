package com.daesabu.meongcoach.alert.adapter.integration;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("meongcoach.alert.slack")
public record SlackAlertProperties(
		boolean enabled,
		String webhookUrl
) {

	@AssertTrue(message = "Slack 알림을 켜면 webhook URL이 필요합니다")
	public boolean isWebhookUrlPresentWhenEnabled() {
		if (!enabled) {
			return true;
		}
		return StringUtils.hasText(webhookUrl);
	}
}
