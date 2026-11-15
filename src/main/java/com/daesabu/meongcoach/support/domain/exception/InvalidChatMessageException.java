package com.daesabu.meongcoach.support.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidChatMessageException extends DomainException {
	public InvalidChatMessageException() {
		super(SupportErrorCode.SUPPORT_CHAT_MESSAGE_INVALID);
	}
}
