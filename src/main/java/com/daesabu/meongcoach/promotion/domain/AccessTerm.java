package com.daesabu.meongcoach.promotion.domain;

import static org.springframework.util.Assert.state;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;

@Embeddable
public record AccessTerm(
		@Column(name = "access_period", length = 20) String period,
		@Column(name = "access_ends_at") Instant endsAt
) {

	private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

	public AccessTerm {
		state((period == null) != (endsAt == null), "혜택 기간과 종료 시각 중 하나만 있어야 합니다");
		validatePeriod(period);
	}

	private static void validatePeriod(String period) {
		if (period == null) {
			return;
		}
		Period parsed = Period.parse(period);
		state(!parsed.isZero() && !parsed.isNegative(), "혜택 기간은 0보다 길어야 합니다");
	}

	public Instant endsAtFrom(Instant redeemedAt) {
		if (endsAt != null) {
			return endsAt;
		}
		return redeemedAt.atZone(KOREA)
				.plus(Period.parse(period))
				.toInstant();
	}
}
