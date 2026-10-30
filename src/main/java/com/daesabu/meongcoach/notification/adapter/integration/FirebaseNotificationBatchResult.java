package com.daesabu.meongcoach.notification.adapter.integration;

import com.daesabu.meongcoach.notification.application.required.NotificationSender.NotificationBatchResult;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FirebaseNotificationBatchResult implements NotificationBatchResult {
	private static final Set<MessagingErrorCode> INVALID_REGISTRATION_CODES =
			Set.of(MessagingErrorCode.UNREGISTERED, MessagingErrorCode.SENDER_ID_MISMATCH);

	private final List<String> invalidFids;

	private FirebaseNotificationBatchResult(List<String> invalidFids) {
		this.invalidFids = invalidFids;
	}

	public static FirebaseNotificationBatchResult success(List<String> fids, List<SendResponse> responses) {
		List<String> invalidFids = new ArrayList<>();
		for (int index = 0; index < responses.size(); index++) {
			FirebaseMessagingException exception = responses.get(index).getException();
			if (exception != null && INVALID_REGISTRATION_CODES.contains(exception.getMessagingErrorCode())) {
				invalidFids.add(fids.get(index));
			}
		}
		invalidFids = List.copyOf(invalidFids);
		return new FirebaseNotificationBatchResult(invalidFids);
	}

	@Override
	public List<String> invalidFids() {
		return invalidFids;
	}

}
