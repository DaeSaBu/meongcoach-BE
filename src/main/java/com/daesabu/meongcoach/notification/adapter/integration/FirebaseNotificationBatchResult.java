package com.daesabu.meongcoach.notification.adapter.integration;

import com.daesabu.meongcoach.notification.application.required.NotificationSender.NotificationBatchResult;
import com.daesabu.meongcoach.notification.domain.PushNotification;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FirebaseNotificationBatchResult implements NotificationBatchResult {
	private final int sentCount;
	private final int failedCount;
	private final List<String> invalidFids;
	private final Throwable failure;

	private FirebaseNotificationBatchResult(int sentCount, int failedCount, List<String> invalidFids, Throwable failure) {
		this.sentCount = sentCount;
		this.failedCount = failedCount;
		this.invalidFids = invalidFids;
		this.failure = failure;
	}

	public static FirebaseNotificationBatchResult success(List<PushNotification> notifications, List<SendResponse> responses) {
		int sentCount = 0;
		int failedCount = 0;
		List<String> invalidFids = new ArrayList<>();
		for (int index = 0; index < responses.size(); index++) {
			FirebaseMessagingException exception = responses.get(index).getException();
			if (exception == null) {
				sentCount++;
				continue;
			}
			if (isInvalid(exception.getMessagingErrorCode())) {
				invalidFids.add(notifications.get(index).fid());
				continue;
			}
			failedCount++;
		}
		return new FirebaseNotificationBatchResult(sentCount, failedCount, List.copyOf(invalidFids), null);
	}

	public static FirebaseNotificationBatchResult failure(List<PushNotification> notifications, Throwable cause) {
		return new FirebaseNotificationBatchResult(0, notifications.size(), List.of(), cause);
	}

	private static boolean isInvalid(MessagingErrorCode code) {
		return code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.SENDER_ID_MISMATCH;
	}

	@Override
	public int sentCount() {
		return sentCount;
	}

	@Override
	public int failedCount() {
		return failedCount;
	}

	@Override
	public List<String> invalidFids() {
		return invalidFids;
	}

	@Override
	public Optional<Throwable> failure() {
		return Optional.ofNullable(failure);
	}
}
