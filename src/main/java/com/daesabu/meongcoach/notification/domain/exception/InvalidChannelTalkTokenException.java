package com.daesabu.meongcoach.notification.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidChannelTalkTokenException extends DomainException {
	public InvalidChannelTalkTokenException() {
		super(NotificationErrorCode.NOTIFICATION_CHANNEL_TALK_TOKEN_INVALID);
	}
}
