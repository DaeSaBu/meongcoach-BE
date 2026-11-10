package com.daesabu.meongcoach.support.application;

import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.EventNotificationDispatchRequest;
import com.daesabu.meongcoach.support.application.provided.ChatPushDispatcher;
import com.daesabu.meongcoach.support.application.provided.ChatPushRegister;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatPushDispatchService implements ChatPushDispatcher {
	private final ChatPushRegister jobs;
	private final EventNotificationDispatcher notifications;

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public boolean dispatchNext() {
		var claimed = jobs.claim();
		if (claimed.isEmpty()) {
			return false;
		}
		var job = claimed.get();
		try {
			var request =
					new EventNotificationDispatchRequest(
							"SUPPORT_REPLY",
							"USERS",
							List.of(job.userId()),
							"문의하신 내용에 답변이 왔어요",
							"문의 화면에서 답변을 확인해 주세요.",
							"meongcoach://support-chat");
			var result = notifications.dispatch(request);
			if (result.retryableFailures() > 0) {
				jobs.fail(job, "PARTIAL_DELIVERY");
				log.warn("상담 푸시 부분 실패: messageId={}, attempt={}", job.messageId(), job.attempts());
				return true;
			}
			if (!jobs.complete(job)) {
				log.warn(
						"상담 푸시 완료 확인 거절: messageId={}, attempt={}",
						job.messageId(),
						job.attempts());
			}
		} catch (RuntimeException exception) {
			jobs.fail(job, exception.getClass().getSimpleName());
			log.warn(
					"상담 푸시 발송 재시도 또는 종료: messageId={}, attempt={}, error={}",
					job.messageId(),
					job.attempts(),
					exception.getClass().getSimpleName());
		}
		return true;
	}
}
