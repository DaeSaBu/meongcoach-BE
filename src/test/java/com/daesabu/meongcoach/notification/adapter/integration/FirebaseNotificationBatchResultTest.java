package com.daesabu.meongcoach.notification.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.daesabu.meongcoach.notification.domain.NotificationDeepLink;
import com.daesabu.meongcoach.notification.domain.PushNotification;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class FirebaseNotificationBatchResultTest {
	@Test
	void 원본_응답을_받아_무효_FID만_추출한다() {
		SendResponse success = mock(SendResponse.class);
		given(success.isSuccessful()).willReturn(true);
		SendResponse unregistered = failure(MessagingErrorCode.UNREGISTERED);
		var results = FirebaseNotificationBatchResult.success(
				notifications("success", "invalid"), List.of(success, unregistered));

		assertThat(results.invalidFids()).containsExactly("invalid");
	}

	@Test
	void SENDER_ID_MISMATCH도_무효_FID으로_추출한다() {
		SendResponse mismatch = failure(MessagingErrorCode.SENDER_ID_MISMATCH);
		var results = FirebaseNotificationBatchResult.success(notifications("mismatch"), List.of(mismatch));

		assertThat(results.invalidFids()).containsExactly("mismatch");
	}

	@Test
	void 무효_등록_외_개별_실패는_건너뛰고_예외를_던지지_않는다() {
		SendResponse unavailable = failure(MessagingErrorCode.UNAVAILABLE);
		SendResponse invalidArgument = failure(MessagingErrorCode.INVALID_ARGUMENT);
		SendResponse unregistered = failure(MessagingErrorCode.UNREGISTERED);
		var results = FirebaseNotificationBatchResult.success(
				notifications("unavailable", "invalid-argument", "unregistered"),
				List.of(unavailable, invalidArgument, unregistered));

		assertThat(results.invalidFids()).containsExactly("unregistered");
	}

	@Test
	void 성공과_무효_외_실패를_따로_센다() {
		SendResponse success = mock(SendResponse.class);
		SendResponse unavailable = failure(MessagingErrorCode.UNAVAILABLE);
		SendResponse unregistered = failure(MessagingErrorCode.UNREGISTERED);
		var results = FirebaseNotificationBatchResult.success(
				notifications("sent", "unavailable", "unregistered"), List.of(success, unavailable, unregistered));

		assertThat(results.sentCount()).isEqualTo(1);
		assertThat(results.failedCount()).isEqualTo(1);
		assertThat(results.invalidFids()).containsExactly("unregistered");
		assertThat(results.failure()).isEmpty();
	}

	@Test
	void 배치_호출_실패는_전체를_실패로_세고_원인을_남긴다() {
		var cause = new IllegalStateException("FCM unavailable");
		var results = FirebaseNotificationBatchResult.failure(notifications("a", "b"), cause);

		assertThat(results.sentCount()).isZero();
		assertThat(results.failedCount()).isEqualTo(2);
		assertThat(results.invalidFids()).isEmpty();
		assertThat(results.failure()).contains(cause);
	}

	@Test
	void 빈_응답은_무효_FID와_실패가_없다() {
		var results = FirebaseNotificationBatchResult.success(List.of(), List.of());

		assertThat(results.invalidFids()).isEmpty();
	}

	private List<PushNotification> notifications(String... fids) {
		return Stream.of(fids).map(fid -> new PushNotification(fid, "제목", "본문", null, null, new NotificationDeepLink(null), "key")).toList();
	}

	private SendResponse failure(MessagingErrorCode code) {
		SendResponse response = mock(SendResponse.class);
		FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
		given(exception.getMessagingErrorCode()).willReturn(code);
		given(response.getException()).willReturn(exception);
		return response;
	}
}
