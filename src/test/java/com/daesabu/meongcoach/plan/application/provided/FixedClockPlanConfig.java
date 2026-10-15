package com.daesabu.meongcoach.plan.application.provided;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class FixedClockPlanConfig {

	static final Instant MIDNIGHT_SEOUL = Instant.parse("2026-10-14T15:30:00Z");

	@Bean
	@Primary
	Clock fixedClock() {
		return Clock.fixed(MIDNIGHT_SEOUL, ZoneId.of("Asia/Seoul"));
	}
}
