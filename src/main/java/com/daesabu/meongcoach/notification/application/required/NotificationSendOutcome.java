package com.daesabu.meongcoach.notification.application.required;

import static java.util.Objects.requireNonNull;

public record NotificationSendOutcome(String fid, NotificationSendStatus status, Throwable cause) {

	public NotificationSendOutcome {
		requireNonNull(fid);
		requireNonNull(status);
	}

	public static NotificationSendOutcome sent(String fid) {
		return new NotificationSendOutcome(fid, NotificationSendStatus.SENT, null);
	}

	public static NotificationSendOutcome invalid(String fid, Throwable cause) {
		return new NotificationSendOutcome(fid, NotificationSendStatus.INVALID, cause);
	}

	public static NotificationSendOutcome failed(String fid, Throwable cause) {
		return new NotificationSendOutcome(fid, NotificationSendStatus.FAILED, cause);
	}

	public static NotificationSendOutcome aborted(String fid, Throwable cause) {
		return new NotificationSendOutcome(fid, NotificationSendStatus.ABORTED, cause);
	}

	public boolean is(NotificationSendStatus expected) {
		return status == expected;
	}
}
