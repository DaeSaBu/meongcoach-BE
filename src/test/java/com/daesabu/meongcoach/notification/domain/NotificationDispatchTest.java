package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class NotificationDispatchTest {
	@Test
	void 진행_중인_발송은_다시_시작하지_않는다() {
		NotificationDispatch dispatch = NotificationDispatch.start("scheduled-key", Instant.now());

		assertThat(dispatch.retry()).isFalse();
	}

	@Test
	void 실패한_발송은_다시_시작할_수_있다() {
		NotificationDispatch dispatch = NotificationDispatch.start("scheduled-key", Instant.now());
		dispatch.fail();

		assertThat(dispatch.retry()).isTrue();
		assertThat(dispatch.getStatus()).isEqualTo(NotificationDispatchStatus.RUNNING);
	}

	@Test
	void 완료된_발송은_다시_시작하지_않는다() {
		NotificationDispatch dispatch = NotificationDispatch.start("scheduled-key", Instant.now());
		dispatch.complete();

		assertThat(dispatch.retry()).isFalse();
		assertThat(dispatch.getStatus()).isEqualTo(NotificationDispatchStatus.COMPLETED);
	}

	@Test
	void 완료된_발송을_실패로_바꿀_수_없다() {
		NotificationDispatch dispatch = NotificationDispatch.start("scheduled-key", Instant.now());
		dispatch.complete();

		assertThatThrownBy(dispatch::fail).isInstanceOf(IllegalStateException.class);
	}
}
