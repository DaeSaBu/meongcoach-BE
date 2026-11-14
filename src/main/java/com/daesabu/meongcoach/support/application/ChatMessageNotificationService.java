package com.daesabu.meongcoach.support.application;

import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.EventNotificationDispatchRequest;
import com.daesabu.meongcoach.support.domain.ChatMessageSentEvent;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatMessageNotificationService {
	private final EventNotificationDispatcher notifications;
	private final UserFinder users;

	@TransactionalEventListener
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void notifyMessage(ChatMessageSentEvent event) {
		try {
			List<Long> recipients = recipients(event);
			if (recipients.isEmpty()) {
				return;
			}
			var request = new EventNotificationDispatchRequest(
					"SUPPORT_REPLY", "USERS", recipients,
					"새 상담 메시지가 도착했어요", "상담 화면에서 메시지를 확인해 주세요.",
					"meongcoach://support-chat");
			notifications.dispatch(request);
		} catch (RuntimeException exception) {
			log.warn("상담 푸시 발송 실패: messageId={}, error={}",
					event.messageId(), exception.getClass().getSimpleName());
		}
	}

	private List<Long> recipients(ChatMessageSentEvent event) {
		if (event.senderId() != event.conversationId()) {
			return List.of(event.conversationId());
		}
		// 임시 처리: ADMIN·EXPERT 전체 발송은 담당 상담원 지정 방식 도입 시 반드시 제거하고 해당 대화의 수신자만 조회해야 한다.
		return users.findActiveOperatorIds().stream()
				.filter(userId -> userId != event.senderId())
				.toList();
	}
}
