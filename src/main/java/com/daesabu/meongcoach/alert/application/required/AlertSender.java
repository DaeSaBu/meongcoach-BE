package com.daesabu.meongcoach.alert.application.required;

import com.daesabu.meongcoach.alert.domain.Alert;

/**
 * 알림 하나를 개발자 채널로 보낸다. 전송에 실패하면 AlertDeliveryFailedException을 던진다.
 */
public interface AlertSender {

	void send(Alert alert);
}
