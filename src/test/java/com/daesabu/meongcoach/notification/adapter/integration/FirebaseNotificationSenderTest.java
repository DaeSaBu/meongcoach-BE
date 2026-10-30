package com.daesabu.meongcoach.notification.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
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

		var result = sender(messaging).send(List.of("expired", "keep"), 17);

		assertThat(result.invalidFids()).containsExactly("expired");
		assertThat(result.failure()).hasCause(badRequest.getException());
	}

	@Test
	void FID을_500개씩_나누어_전송한다() throws Exception {
		FirebaseMessaging messaging = mock(FirebaseMessaging.class);
		BatchResponse batch = mock(BatchResponse.class);
		given(batch.getResponses()).willReturn(List.of());
		given(messaging.sendEach(anyList())).willReturn(batch);
		List<String> fids = IntStream.range(0, 501).mapToObj(i -> "fid-" + i).toList();

		sender(messaging).send(fids, 17);

		ArgumentCaptor<List<com.google.firebase.messaging.Message>> captor = ArgumentCaptor.captor();
		verify(messaging, times(2)).sendEach(captor.capture());
		assertThat(captor.getAllValues()).extracting(List::size).containsExactly(500, 1);
	}

	@Test
	void 배치_요청_실패를_집계하고_뒤의_배치도_시도한다() throws Exception {
		FirebaseMessaging messaging = mock(FirebaseMessaging.class);
		FirebaseMessagingException failure = mock(FirebaseMessagingException.class);
		BatchResponse batch = mock(BatchResponse.class);
		SendResponse success = mock(SendResponse.class);
		given(success.isSuccessful()).willReturn(true);
		given(batch.getResponses()).willReturn(List.of(success));
		given(messaging.sendEach(anyList())).willThrow(failure).willReturn(batch);

		var result = sender(messaging).send(Collections.nCopies(501, "fid"), 17);

		assertThat(result.failure()).hasCause(failure);
		verify(messaging, times(2)).sendEach(anyList());
	}

	@Test
	void 기존_앱이_읽는_시각과_딥링크를_플랫폼_설정과_함께_보낸다() throws Exception {
		FirebaseMessaging messaging = mock(FirebaseMessaging.class);
		BatchResponse batch = mock(BatchResponse.class);
		given(batch.getResponses()).willReturn(List.of());
		given(messaging.sendEach(anyList())).willReturn(batch);
		sender(messaging).send(List.of("fid"), 17);

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
	void 여러_배치의_무효_FID과_실패_수를_합치고_첫_원인을_유지한다() throws Exception {
		FirebaseMessaging messaging = mock(FirebaseMessaging.class);
		SendResponse success = mock(SendResponse.class);
		given(success.isSuccessful()).willReturn(true);
		SendResponse expired = failure(MessagingErrorCode.UNREGISTERED);
		SendResponse firstFailure = failure(MessagingErrorCode.INVALID_ARGUMENT);
		SendResponse laterFailure = failure(MessagingErrorCode.UNAVAILABLE);
		List<SendResponse> firstResponses = new ArrayList<>(Collections.nCopies(499, success));
		firstResponses.add(expired);
		List<SendResponse> secondResponses = new ArrayList<>(Collections.nCopies(498, success));
		secondResponses.add(expired);
		secondResponses.add(firstFailure);
		BatchResponse first = mock(BatchResponse.class);
		BatchResponse second = mock(BatchResponse.class);
		BatchResponse third = mock(BatchResponse.class);
		given(first.getResponses()).willReturn(firstResponses);
		given(second.getResponses()).willReturn(secondResponses);
		given(third.getResponses()).willReturn(List.of(laterFailure));
		given(messaging.sendEach(anyList())).willReturn(first, second, third);
		List<String> fids = IntStream.range(0, 1001).mapToObj(index -> "fid-" + index).toList();

		var result = sender(messaging).send(fids, 17);

		assertThat(result.invalidFids()).containsExactly("fid-499", "fid-998");
		assertThat(result.failure()).hasCause(firstFailure.getException());
		verify(messaging, times(3)).sendEach(anyList());
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
