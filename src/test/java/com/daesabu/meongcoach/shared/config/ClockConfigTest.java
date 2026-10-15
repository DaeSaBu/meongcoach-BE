package com.daesabu.meongcoach.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validator;
import jakarta.validation.constraints.PastOrPresent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@SpringBootTest
@Import(ClockConfigTest.FarFutureClockConfig.class)
class ClockConfigTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	@Autowired
	private Validator validator;

	@Autowired
	private Clock clock;

	@Test
	void 운영_시계는_서울_시간대를_쓴다() {
		assertThat(new ClockConfig().clock().getZone()).isEqualTo(SEOUL);
	}

	@Test
	void Bean_Validation의_현재_시각도_주입된_시계를_따른다() {
		assertThat(clock.getZone()).isEqualTo(SEOUL);

		assertThat(validator.validate(new Holder(LocalDate.of(2099, 1, 2)))).isEmpty();
		assertThat(validator.validate(new Holder(LocalDate.of(2099, 1, 3)))).hasSize(1);
	}

	record Holder(@PastOrPresent LocalDate birthDate) {
	}

	@TestConfiguration
	static class FarFutureClockConfig {

		@Bean
		@Primary
		Clock farFutureClock() {
			return Clock.fixed(Instant.parse("2099-01-01T15:30:00Z"), SEOUL);
		}
	}
}
