package com.daesabu.meongcoach.support.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class ChatClosedException extends DomainException {
	public ChatClosedException() {
		super(SupportErrorCode.SUPPORT_CHAT_CLOSED);
	}
}
