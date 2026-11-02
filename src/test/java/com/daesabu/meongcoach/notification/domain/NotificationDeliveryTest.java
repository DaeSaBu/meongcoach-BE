package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationDeliveryTest {

	@Test
	void 두_전달_결과를_합치면_수는_더하고_수신_불가_FID은_순서대로_이어붙인다() {
		var first = new NotificationDelivery(499, 0, List.of("fid-1"));
		var second = new NotificationDelivery(1, 3, List.of("fid-501", "fid-502"));

		NotificationDelivery sum = first.plus(second);

		assertThat(sum.deliveredCount()).isEqualTo(500);
		assertThat(sum.failedCount()).isEqualTo(3);
		assertThat(sum.unreachableFids()).containsExactly("fid-1", "fid-501", "fid-502");
		assertThat(first.unreachableFids()).containsExactly("fid-1");
	}

	@Test
	void 빈_결과에_합치면_그대로다() {
		var delivery = new NotificationDelivery(2, 1, List.of("fid-1"));

		assertThat(NotificationDelivery.none().plus(delivery)).isEqualTo(delivery);
	}

	@Test
	void 수신_불가_FID이_있는지_알려준다() {
		assertThat(new NotificationDelivery(1, 0, List.of("fid-1")).hasUnreachable()).isTrue();
		assertThat(new NotificationDelivery(1, 0, List.of()).hasUnreachable()).isFalse();
	}

	@Test
	void 전달된_기기가_없고_실패가_있으면_던진다() {
		var delivery = new NotificationDelivery(0, 2, List.of("fid-1"));

		assertThatThrownBy(delivery::requireDelivered).isInstanceOf(NotificationDeliveryException.class);
	}

	@Test
	void 수신_불가만_있거나_하나라도_전달됐으면_던지지_않는다() {
		assertThatCode(new NotificationDelivery(0, 0, List.of("fid-1"))::requireDelivered).doesNotThrowAnyException();
		assertThatCode(new NotificationDelivery(1, 5, List.of())::requireDelivered).doesNotThrowAnyException();
		assertThatCode(NotificationDelivery.none()::requireDelivered).doesNotThrowAnyException();
	}

	@Test
	void 수신_불가_FID_목록은_바꿀_수_없다() {
		var delivery = new NotificationDelivery(0, 0, List.of("fid-1"));

		assertThatThrownBy(() -> delivery.unreachableFids().add("fid-2")).isInstanceOf(UnsupportedOperationException.class);
	}
}
