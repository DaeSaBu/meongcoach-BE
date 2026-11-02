package com.daesabu.meongcoach.notification.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationSendResultTest {

	private static final Throwable CAUSE = new IllegalStateException("FCM unavailable");

	@Test
	void 무효_FID을_순서대로_모은다() {
		var result = new NotificationSendResult(List.of(
				NotificationSendOutcome.sent("fid-1"),
				NotificationSendOutcome.invalid("fid-2", CAUSE),
				NotificationSendOutcome.failed("fid-3", CAUSE),
				NotificationSendOutcome.invalid("fid-4", CAUSE)));

		assertThat(result.invalidFids()).containsExactly("fid-2", "fid-4");
	}

	@Test
	void 중단된_알림이_있으면_그_원인을_돌려준다() {
		var result = new NotificationSendResult(List.of(
				NotificationSendOutcome.sent("fid-1"),
				NotificationSendOutcome.aborted("fid-2", CAUSE),
				NotificationSendOutcome.aborted("fid-3", CAUSE)));

		assertThat(result.aborted()).isTrue();
		assertThat(result.abortCause()).isSameAs(CAUSE);
	}

	@Test
	void 중단된_알림이_없으면_원인도_없다() {
		var result = new NotificationSendResult(List.of(
				NotificationSendOutcome.sent("fid-1"),
				NotificationSendOutcome.failed("fid-2", CAUSE)));

		assertThat(result.aborted()).isFalse();
		assertThat(result.abortCause()).isNull();
	}

	@Test
	void 전송된_알림이_없고_실패가_있으면_모두_실패다() {
		var onlyFailed = new NotificationSendResult(List.of(
				NotificationSendOutcome.failed("fid-1", CAUSE),
				NotificationSendOutcome.failed("fid-2", CAUSE)));
		var failedWithInvalid = new NotificationSendResult(List.of(
				NotificationSendOutcome.failed("fid-1", CAUSE),
				NotificationSendOutcome.invalid("fid-2", CAUSE)));

		assertThat(onlyFailed.allFailed()).isTrue();
		assertThat(failedWithInvalid.allFailed()).isTrue();
	}

	@Test
	void 무효_FID만_있거나_하나라도_전송됐으면_모두_실패가_아니다() {
		var onlyInvalid = new NotificationSendResult(List.of(NotificationSendOutcome.invalid("fid-1", CAUSE)));
		var partlySent = new NotificationSendResult(List.of(
				NotificationSendOutcome.sent("fid-1"),
				NotificationSendOutcome.failed("fid-2", CAUSE)));
		var empty = new NotificationSendResult(List.of());

		assertThat(onlyInvalid.allFailed()).isFalse();
		assertThat(partlySent.allFailed()).isFalse();
		assertThat(empty.allFailed()).isFalse();
	}
}
