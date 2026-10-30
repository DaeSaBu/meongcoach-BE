package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationDeliveryResultsTest {
	@Test
	void 무효_FID만_추출하고_등록_해제는_실패로_판단하지_않는다() {
		var results = new NotificationDeliveryResults(List.of(
				new NotificationDeliveryResult("success", false, null),
				new NotificationDeliveryResult("invalid", true, null)));

		assertThat(results.invalidFids()).containsExactly("invalid");
		assertThatCode(results::throwIfFailed).doesNotThrowAnyException();
	}

	@Test
	void 실패가_여러_개면_첫_원인을_그대로_전파한다() {
		var first = new NotificationDeliveryException(new IllegalStateException("first"));
		var later = new NotificationDeliveryException(new IllegalStateException("later"));
		var results = new NotificationDeliveryResults(List.of(
				new NotificationDeliveryResult("first", false, first),
				new NotificationDeliveryResult("later", false, later)));

		assertThatThrownBy(results::throwIfFailed).isSameAs(first);
	}

	@Test
	void 빈_결과는_무효_FID와_실패가_없다() {
		var results = new NotificationDeliveryResults(List.of());

		assertThat(results.invalidFids()).isEmpty();
		assertThatCode(results::throwIfFailed).doesNotThrowAnyException();
	}
}
