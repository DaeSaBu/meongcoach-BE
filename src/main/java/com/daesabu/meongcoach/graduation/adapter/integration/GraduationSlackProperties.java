package com.daesabu.meongcoach.graduation.adapter.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("meongcoach.graduation.slack")
public record GraduationSlackProperties(String webhookUrl) {

	public boolean isEnabled() {
		return webhookUrl != null && !webhookUrl.isBlank();
	}
}
