package com.daesabu.meongcoach.notification.application.required;

import java.util.List;

public interface NotificationBatchResult {
	/** 무효로 판정된 FID 목록을 반환한다. */
	List<String> invalidFids();

	/** 발송 실패가 있으면 첫 실패 원인을 담은 예외를 던진다. */
	void throwIfFailed();
}
