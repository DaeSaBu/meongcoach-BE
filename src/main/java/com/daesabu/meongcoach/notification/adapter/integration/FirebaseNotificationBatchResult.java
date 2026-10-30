package com.daesabu.meongcoach.notification.adapter.integration;

import com.daesabu.meongcoach.notification.application.required.NotificationBatchResult;
import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import java.util.ArrayList;
import java.util.List;

public class FirebaseNotificationBatchResult implements NotificationBatchResult {
	private final List<String> invalidFids;
	private final FirebaseMessagingException failure;

	private FirebaseNotificationBatchResult(List<String> invalidFids, FirebaseMessagingException failure) {
		this.invalidFids = invalidFids;
		this.failure = failure;
	}

	public static FirebaseNotificationBatchResult success(List<String> fids, List<SendResponse> responses) {
		List<String> invalidFids = new ArrayList<>();
		FirebaseMessagingException failure = null;
		for (int index = 0; index < responses.size(); index++) {
			FirebaseMessagingException exception = responses.get(index).getException();
			if (exception == null) {
				continue;
			}
			if (exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
				invalidFids.add(fids.get(index));
				continue;
			}
			if (failure == null) {
				failure = exception;
			}
		}
		invalidFids = List.copyOf(invalidFids);
		return new FirebaseNotificationBatchResult(invalidFids, failure);
	}

	public static FirebaseNotificationBatchResult failed(FirebaseMessagingException failure) {
		List<String> invalidFids = List.of();
		return new FirebaseNotificationBatchResult(invalidFids, failure);
	}

	@Override
	public List<String> invalidFids() {
		return invalidFids;
	}

	@Override
	public void throwIfFailed() {
		if (failure != null) {
			throw new NotificationDeliveryException(failure);
		}
	}
}
