package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.training.domain.exception.InvalidTrainingCalendarPeriodException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TrainingCalendarPeriodTest {

	private static final LocalDate FROM = LocalDate.of(2026, 11, 1);

	@Test
	void 시작일과_종료일이_같은_하루도_조회_기간이다() {
		TrainingCalendarPeriod period = new TrainingCalendarPeriod(FROM, FROM);

		assertThat(period.contains(FROM)).isTrue();
	}

	@Test
	void 시작일부터_62일째_날까지_조회할_수_있다() {
		LocalDate to = FROM.plusDays(61);

		TrainingCalendarPeriod period = new TrainingCalendarPeriod(FROM, to);

		assertThat(period.to()).isEqualTo(to);
	}

	@Test
	void 조회_기간이_62일을_넘으면_예외를_던진다() {
		LocalDate to = FROM.plusDays(62);

		assertThatThrownBy(() -> new TrainingCalendarPeriod(FROM, to))
				.isInstanceOf(InvalidTrainingCalendarPeriodException.class);
	}

	@Test
	void 시작일이_종료일보다_늦으면_예외를_던진다() {
		LocalDate to = FROM.minusDays(1);

		assertThatThrownBy(() -> new TrainingCalendarPeriod(FROM, to))
				.isInstanceOf(InvalidTrainingCalendarPeriodException.class);
	}

	@Test
	void 시작일과_종료일은_기간에_포함되고_그_밖의_날은_포함되지_않는다() {
		LocalDate to = FROM.plusDays(6);
		TrainingCalendarPeriod period = new TrainingCalendarPeriod(FROM, to);

		assertThat(period.contains(FROM)).isTrue();
		assertThat(period.contains(to)).isTrue();
		assertThat(period.contains(FROM.minusDays(1))).isFalse();
		assertThat(period.contains(to.plusDays(1))).isFalse();
	}
}
