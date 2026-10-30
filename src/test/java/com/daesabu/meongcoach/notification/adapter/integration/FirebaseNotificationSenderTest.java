package com.daesabu.meongcoach.notification.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.util.LinkedHashMap;
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
		SendResponse success = mock(SendResponse.class);
		given(batch.getResponses()).willReturn(List.of(expired, success));
		given(messaging.sendEach(anyList())).willReturn(batch);

		var result = sender(messaging).send(messages(List.of("expired", "keep"))).getFirst();

		assertThat(result.invalidFids()).containsExactly("expired");
	}

	@Test
	void 배치_호출이_실패하면_즉시_중단한다() throws Exception {
		FirebaseMessaging messaging = mock(FirebaseMessaging.class);
		FirebaseMessagingException failure = mock(FirebaseMessagingException.class);
		given(messaging.sendEach(anyList())).willThrow(failure);
		List<String> fids = java.util.stream.IntStream.range(0, 501).mapToObj(index -> "fid-" + index).toList();

		assertThatThrownBy(() -> sender(messaging).send(messages(fids))).hasCause(failure);

		verify(messaging, times(1)).sendEach(anyList());
	}

	@Test
	void 개별_발송이_실패해도_다음_배치는_보내지_않는다() throws Exception {
		FirebaseMessaging messaging = mock(FirebaseMessaging.class);
		BatchResponse batch = mock(BatchResponse.class);
		SendResponse failed = failure(MessagingErrorCode.UNAVAILABLE);
		given(batch.getResponses()).willReturn(List.of(failed));
		given(messaging.sendEach(anyList())).willReturn(batch);
		List<String> fids = java.util.stream.IntStream.range(0, 501).mapToObj(index -> "fid-" + index).toList();

		assertThatThrownBy(() -> sender(messaging).send(messages(fids))).hasCause(failed.getException());

		verify(messaging, times(1)).sendEach(anyList());
	}

	@Test
	void 기존_앱이_읽는_시각과_딥링크를_플랫폼_설정과_함께_보낸다() throws Exception {
		FirebaseMessaging messaging = mock(FirebaseMessaging.class);
		BatchResponse batch = mock(BatchResponse.class);
		given(batch.getResponses()).willReturn(List.of());
		given(messaging.sendEach(anyList())).willReturn(batch);
		sender(messaging).send(messages(List.of("fid")));

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
		SendResponse unregistered = failure(MessagingErrorCode.UNREGISTERED);
		given(messaging.sendEach(anyList())).willAnswer(invocation -> {
			List<?> messages = invocation.getArgument(0);
			BatchResponse batch = mock(BatchResponse.class);
			given(batch.getResponses()).willReturn(java.util.Collections.nCopies(messages.size(), unregistered));
			return batch;
		});
		List<String> fids = java.util.stream.IntStream.range(0, 1001).mapToObj(index -> "fid-" + index).toList();

		var results = sender(messaging).send(messages(fids));

		ArgumentCaptor<List<com.google.firebase.messaging.Message>> batches = ArgumentCaptor.captor();
		verify(messaging, times(3)).sendEach(batches.capture());
		assertThat(batches.getAllValues()).extracting(List::size).containsExactly(500, 500, 1);
		assertThat(results).hasSize(3);
		assertThat(results.stream().flatMap(result -> result.invalidFids().stream()).toList()).containsExactlyElementsOf(fids);
	}

	private Map<String, NotificationMessage> messages(List<String> fids) {
		Map<String, NotificationMessage> messages = new LinkedHashMap<>();
		NotificationMessage message = message();
		fids.forEach(fid -> messages.put(fid, message));
		return messages;
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
