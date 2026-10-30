package com.daesabu.meongcoach.notification.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.daesabu.meongcoach.notification.domain.NotificationMessage;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

class FirebaseNotificationSenderTest {

	@Test
	void UNREGISTERED만_무효_FID으로_반환한다() throws Exception {
		FirebaseMessaging messaging = mock(FirebaseMessaging.class);
		BatchResponse batch = mock(BatchResponse.class);
		SendResponse expired = failure(MessagingErrorCode.UNREGISTERED);
		SendResponse badRequest = failure(MessagingErrorCode.INVALID_ARGUMENT);
		given(batch.getResponses()).willReturn(List.of(expired, badRequest));
		given(messaging.sendEach(anyList())).willReturn(batch);

		var result = sender(messaging).send(List.of("expired", "keep"), message());

		assertThat(result).extracting(com.daesabu.meongcoach.notification.domain.NotificationDeliveryResult::fid)
				.containsExactly("expired", "keep");
		assertThat(result.getFirst().invalid()).isTrue();
		assertThat(result.getFirst().failure()).isNull();
		assertThat(result.getLast().invalid()).isFalse();
		assertThat(result.getLast().failure()).hasCause(badRequest.getException());
	}

	@Test
	void 배치_실패를_개별_결과로_변환하고_다음_배치도_호출한다() throws Exception {
		FirebaseMessaging messaging = mock(FirebaseMessaging.class);
		FirebaseMessagingException failure = mock(FirebaseMessagingException.class);
		BatchResponse batch = mock(BatchResponse.class);
		SendResponse success = mock(SendResponse.class);
		given(success.isSuccessful()).willReturn(true);
		given(batch.getResponses()).willReturn(List.of(success));
		given(messaging.sendEach(anyList())).willThrow(failure).willReturn(batch);

		var result = sender(messaging).send(java.util.stream.IntStream.range(0, 501).mapToObj(index -> "fid-" + index).toList(), message());

		assertThat(result).hasSize(501);
		assertThat(result.getFirst().failure()).hasCause(failure);
		assertThat(result.get(499).failure()).hasCause(failure);
		assertThat(result.getLast().fid()).isEqualTo("fid-500");
		assertThat(result.getLast().failure()).isNull();
		verify(messaging, times(2)).sendEach(anyList());
	}

	@Test
	void 기존_앱이_읽는_시각과_딥링크를_플랫폼_설정과_함께_보낸다() throws Exception {
		FirebaseMessaging messaging = mock(FirebaseMessaging.class);
		BatchResponse batch = mock(BatchResponse.class);
		given(batch.getResponses()).willReturn(List.of());
		given(messaging.sendEach(anyList())).willReturn(batch);
		sender(messaging).send(List.of("fid"), message());

		ArgumentCaptor<List<com.google.firebase.messaging.Message>> captor = ArgumentCaptor.captor();
		verify(messaging).sendEach(captor.capture());
		String json = com.google.api.client.json.gson.GsonFactory.getDefaultInstance().toString(captor.getValue().getFirst());
		var message = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
		assertThat(message.get("fid").getAsString()).isEqualTo("fid");
		assertThat(message.has("token")).isFalse();
		assertThat(message.getAsJsonObject("data").get("hour").getAsString()).isEqualTo("17");
		assertThat(message.getAsJsonObject("data").get("deepLink").getAsString()).isEqualTo("meongcoach://training-curriculum");
		assertThat(message.getAsJsonObject("notification").get("title").getAsString()).isEqualTo("저녁 훈련 시간이에요");
		assertThat(message.getAsJsonObject("android").get("ttl").getAsString()).isEqualTo("3600s");
		assertThat(message.getAsJsonObject("android").get("priority").getAsString()).isEqualTo("high");
		assertThat(message.getAsJsonObject("apns").getAsJsonObject("headers").get("apns-priority").getAsString()).isEqualTo("10");
	}

	@Test
	void FID을_500개씩_분할하고_응답_순서를_유지한다() throws Exception {
		FirebaseMessaging messaging = mock(FirebaseMessaging.class);
		SendResponse success = mock(SendResponse.class);
		given(success.isSuccessful()).willReturn(true);
		given(messaging.sendEach(anyList())).willAnswer(invocation -> {
			List<?> messages = invocation.getArgument(0);
			BatchResponse batch = mock(BatchResponse.class);
			given(batch.getResponses()).willReturn(java.util.Collections.nCopies(messages.size(), success));
			return batch;
		});
		List<String> fids = java.util.stream.IntStream.range(0, 1001).mapToObj(index -> "fid-" + index).toList();

		var results = sender(messaging).send(fids, message());

		ArgumentCaptor<List<com.google.firebase.messaging.Message>> batches = ArgumentCaptor.captor();
		verify(messaging, times(3)).sendEach(batches.capture());
		assertThat(batches.getAllValues()).extracting(List::size).containsExactly(500, 500, 1);
		assertThat(results).extracting(com.daesabu.meongcoach.notification.domain.NotificationDeliveryResult::fid)
				.containsExactlyElementsOf(fids);
	}

	private NotificationMessage message() {
		Map<String, String> data = Map.of("hour", "17", "deepLink", "meongcoach://training-curriculum");
		return new NotificationMessage("저녁 훈련 시간이에요", "오늘의 훈련을 마무리해 볼까요?", data);
	}

	private SendResponse failure(MessagingErrorCode code) {
		SendResponse response = mock(SendResponse.class);
		FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
		given(exception.getMessagingErrorCode()).willReturn(code);
		given(response.getException()).willReturn(exception);
		return response;
	}

	@SuppressWarnings("unchecked")
	private FirebaseNotificationSender sender(FirebaseMessaging messaging) {
		ObjectProvider<FirebaseMessaging> provider = mock(ObjectProvider.class);
		given(provider.getObject()).willReturn(messaging);
		return new FirebaseNotificationSender(provider);
	}
}
