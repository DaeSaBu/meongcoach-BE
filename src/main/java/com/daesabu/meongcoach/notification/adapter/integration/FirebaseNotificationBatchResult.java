package com.daesabu.meongcoach.notification.adapter.integration;

import com.daesabu.meongcoach.notification.application.required.NotificationSender.BatchResult;
import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import java.util.ArrayList;
import java.util.List;

public class FirebaseNotificationBatchResult implements BatchResult {
	private final List<String> invalidFids;

	private FirebaseNotificationBatchResult(List<String> invalidFids) {
		this.invalidFids = invalidFids;
	}

	public static FirebaseNotificationBatchResult success(List<String> fids, List<SendResponse> responses) {
		List<String> invalidFids = new ArrayList<>();
		for (int index = 0; index < responses.size(); index++) {
			FirebaseMessagingException exception = responses.get(index).getException();
			if (exception == null) {
				continue;
			}
			if (exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
				invalidFids.add(fids.get(index));
				continue;
			}
			throw new NotificationDeliveryException(exception);
		}
		invalidFids = List.copyOf(invalidFids);
		return new FirebaseNotificationBatchResult(invalidFids);
	}

	@Override
	public List<String> invalidFids() {
		return invalidFids;
	}

}
