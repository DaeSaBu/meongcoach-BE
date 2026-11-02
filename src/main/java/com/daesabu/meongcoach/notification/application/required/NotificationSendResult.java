package com.daesabu.meongcoach.notification.application.required;

import java.util.List;

public record NotificationSendResult(List<NotificationSendOutcome> outcomes) {

	public NotificationSendResult {
		outcomes = List.copyOf(outcomes);
	}

	public List<String> invalidFids() {
		return outcomes.stream()
				.filter(outcome -> outcome.is(NotificationSendStatus.INVALID))
				.map(NotificationSendOutcome::fid)
				.toList();
	}

	public boolean aborted() {
		return outcomes.stream().anyMatch(outcome -> outcome.is(NotificationSendStatus.ABORTED));
	}

	public Throwable abortCause() {
		return outcomes.stream()
				.filter(outcome -> outcome.is(NotificationSendStatus.ABORTED))
				.map(NotificationSendOutcome::cause)
				.findFirst()
				.orElse(null);
	}

	public boolean allFailed() {
		boolean anySent = outcomes.stream().anyMatch(outcome -> outcome.is(NotificationSendStatus.SENT));
		boolean anyFailed = outcomes.stream().anyMatch(outcome -> outcome.is(NotificationSendStatus.FAILED));
		return !anySent && anyFailed;
	}
}
