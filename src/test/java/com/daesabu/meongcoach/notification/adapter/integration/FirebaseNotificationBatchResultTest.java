package com.daesabu.meongcoach.notification.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
		assertThatCode(results::throwIfFailed).doesNotThrowAnyException();
	}

	@Test
	void 실패가_여러_개면_첫_원인을_전파한다() {
		SendResponse first = failure(MessagingErrorCode.UNAVAILABLE);
		SendResponse later = failure(MessagingErrorCode.INVALID_ARGUMENT);
		var results = FirebaseNotificationBatchResult.success(
				List.of("first", "later"), List.of(first, later));

		assertThatThrownBy(results::throwIfFailed).hasCause(first.getException());
	}

	@Test
	void 빈_응답은_무효_FID와_실패가_없다() {
		var results = FirebaseNotificationBatchResult.success(List.of(), List.of());

		assertThat(results.invalidFids()).isEmpty();
		assertThatCode(results::throwIfFailed).doesNotThrowAnyException();
	}

	private SendResponse failure(MessagingErrorCode code) {
		SendResponse response = mock(SendResponse.class);
		FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
		given(exception.getMessagingErrorCode()).willReturn(code);
		given(response.getException()).willReturn(exception);
		return response;
	}
}
