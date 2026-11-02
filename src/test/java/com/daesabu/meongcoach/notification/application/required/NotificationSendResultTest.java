package com.daesabu.meongcoach.notification.application.required;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.notification.application.required.NotificationSender.NotificationSendResult;
import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationSendResultTest {

	@Test
	void 배치_결과를_합산하고_첫_실패_원인을_남긴다() {
		var first = new NotificationSendResult(499, 0, List.of("fid-1"), null);
		var cause = new IllegalStateException("FCM unavailable");
		var second = new NotificationSendResult(0, 500, List.of(), cause);
		var third = new NotificationSendResult(1, 0, List.of("fid-1001"), new IllegalStateException("later"));

		NotificationSendResult merged = NotificationSendResult.merge(List.of(first, second, third));

		assertThat(merged.sentCount()).isEqualTo(500);
		assertThat(merged.failedCount()).isEqualTo(500);
		assertThat(merged.invalidFids()).containsExactly("fid-1", "fid-1001");
		assertThat(merged.failure()).isSameAs(cause);
	}

	@Test
	void 여러_배치_실패_원인은_첫_원인에_덧붙인다() {
		var first = new IllegalStateException("first");
		var second = new IllegalStateException("second");
		var results = List.of(new NotificationSendResult(0, 1, List.of(), first), new NotificationSendResult(0, 1, List.of(), second));

		NotificationSendResult merged = NotificationSendResult.merge(results);

		assertThat(merged.failure()).isSameAs(first);
		assertThat(first.getSuppressed()).containsExactly(second);
	}

	@Test
	void 배치_호출이_실패했으면_원인과_함께_던진다() {
		var cause = new IllegalStateException("FCM unavailable");
		var result = new NotificationSendResult(10, 500, List.of(), cause);

		assertThatThrownBy(result::requireDelivered).isInstanceOf(NotificationDeliveryException.class).hasCause(cause);
	}

	@Test
	void 성공한_전송이_없고_무효_외_실패가_있으면_던진다() {
		var result = new NotificationSendResult(0, 2, List.of("invalid"), null);

		assertThatThrownBy(result::requireDelivered).isInstanceOf(NotificationDeliveryException.class);
	}

	@Test
	void 무효_FID만_있거나_일부만_실패하면_던지지_않는다() {
		assertThatCode(new NotificationSendResult(0, 0, List.of("invalid"), null)::requireDelivered).doesNotThrowAnyException();
		assertThatCode(new NotificationSendResult(1, 5, List.of(), null)::requireDelivered).doesNotThrowAnyException();
		assertThatCode(new NotificationSendResult(0, 0, List.of(), null)::requireDelivered).doesNotThrowAnyException();
	}
}
