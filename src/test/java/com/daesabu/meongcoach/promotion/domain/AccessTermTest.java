package com.daesabu.meongcoach.promotion.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.Test;

class AccessTermTest {

	private static final Instant REDEEMED_AT = Instant.parse("2026-10-09T03:00:00Z");
	private static final Instant FIXED_ENDS_AT = Instant.parse("2026-12-02T15:00:00Z");

	@Test
	void 기간과_종료_시각이_모두_없으면_만들_수_없다() {
		assertThatThrownBy(() -> new AccessTerm(null, null))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 기간과_종료_시각을_함께_가질_수_없다() {
		assertThatThrownBy(() -> new AccessTerm("P3M", FIXED_ENDS_AT))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void ISO_8601_형식이_아닌_기간은_만들_수_없다() {
		assertThatThrownBy(() -> new AccessTerm("3개월", null))
				.isInstanceOf(DateTimeParseException.class);
	}

	@Test
	void 길이가_0인_기간은_만들_수_없다() {
		assertThatThrownBy(() -> new AccessTerm("P0D", null))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 음수_기간은_만들_수_없다() {
		assertThatThrownBy(() -> new AccessTerm("P-1M", null))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 기간이면_사용_시각에_기간을_더한_시각에_끝난다() {
		AccessTerm accessTerm = new AccessTerm("P3M", null);

		Instant endsAt = accessTerm.endsAtFrom(REDEEMED_AT);

		assertThat(endsAt).isEqualTo(Instant.parse("2027-01-09T03:00:00Z"));
	}

	@Test
	void 기간의_월은_한국_시간_기준_날짜로_더한다() {
		AccessTerm accessTerm = new AccessTerm("P1M", null);
		Instant januaryLastDayInKorea = Instant.parse("2027-01-30T15:00:00Z");

		Instant endsAt = accessTerm.endsAtFrom(januaryLastDayInKorea);

		assertThat(endsAt).isEqualTo(Instant.parse("2027-02-27T15:00:00Z"));
	}

	@Test
	void 종료_시각이면_사용_시각과_관계없이_그_시각에_끝난다() {
		AccessTerm accessTerm = new AccessTerm(null, FIXED_ENDS_AT);

		Instant endsAt = accessTerm.endsAtFrom(REDEEMED_AT);

		assertThat(endsAt).isEqualTo(FIXED_ENDS_AT);
	}
}
