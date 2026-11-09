package com.daesabu.meongcoach.notification.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class ChannelTalkUnavailableException extends DomainException {
	public ChannelTalkUnavailableException() {
		super(NotificationErrorCode.NOTIFICATION_CHANNEL_TALK_UNAVAILABLE);
	}
}
