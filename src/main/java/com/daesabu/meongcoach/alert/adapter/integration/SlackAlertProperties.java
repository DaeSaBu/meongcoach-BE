package com.daesabu.meongcoach.alert.adapter.integration;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("meongcoach.alert.slack")
public record SlackAlertProperties(
		boolean enabled,
		String graduationWebhookUrl
) {

	@AssertTrue(message = "Slack 알림을 켜면 실습 인증 webhook URL이 필요합니다")
	public boolean isGraduationWebhookUrlPresentWhenEnabled() {
		if (!enabled) {
			return true;
		}
		return StringUtils.hasText(graduationWebhookUrl);
	}
}
