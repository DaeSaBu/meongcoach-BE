package com.daesabu.meongcoach.notification.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class FirebaseNotificationBatchResultTest {
	@Test
	void 원본_응답을_받아_무효_FID만_추출한다() {
		SendResponse success = mock(SendResponse.class);
		given(success.isSuccessful()).willReturn(true);
		SendResponse unregistered = failure(MessagingErrorCode.UNREGISTERED);
		var results = FirebaseNotificationBatchResult.success(
				List.of("success", "invalid"), List.of(success, unregistered));

		assertThat(results.invalidFids()).containsExactly("invalid");
	}

	@Test
	void SENDER_ID_MISMATCH도_무효_FID으로_추출한다() {
		SendResponse mismatch = failure(MessagingErrorCode.SENDER_ID_MISMATCH);
		var results = FirebaseNotificationBatchResult.success(List.of("mismatch"), List.of(mismatch));

		assertThat(results.invalidFids()).containsExactly("mismatch");
	}

	@Test
	void 무효_등록_외_개별_실패는_건너뛰고_예외를_던지지_않는다() {
		SendResponse unavailable = failure(MessagingErrorCode.UNAVAILABLE);
		SendResponse invalidArgument = failure(MessagingErrorCode.INVALID_ARGUMENT);
		SendResponse unregistered = failure(MessagingErrorCode.UNREGISTERED);
		var results = FirebaseNotificationBatchResult.success(
				List.of("unavailable", "invalid-argument", "unregistered"),
				List.of(unavailable, invalidArgument, unregistered));

		assertThat(results.invalidFids()).containsExactly("unregistered");
	}

	@Test
	void 빈_응답은_무효_FID와_실패가_없다() {
		var results = FirebaseNotificationBatchResult.success(List.of(), List.of());

		assertThat(results.invalidFids()).isEmpty();
	}

	private SendResponse failure(MessagingErrorCode code) {
		SendResponse response = mock(SendResponse.class);
		FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
		given(exception.getMessagingErrorCode()).willReturn(code);
		given(response.getException()).willReturn(exception);
		return response;
	}
}
