package com.daesabu.meongcoach.support.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class ChannelTalkUnavailableException extends DomainException {
	public ChannelTalkUnavailableException() {
		super(SupportErrorCode.SUPPORT_CHANNEL_TALK_UNAVAILABLE);
	}
}
