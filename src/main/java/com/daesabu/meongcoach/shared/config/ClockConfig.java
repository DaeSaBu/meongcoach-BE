package com.daesabu.meongcoach.shared.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	@Bean
	public Clock clock() {
		return Clock.system(SEOUL);
	}

	@Bean
	public ValidationConfigurationCustomizer clockProviderCustomizer(Clock clock) {
		return configuration -> configuration.clockProvider(() -> clock);
	}
}
