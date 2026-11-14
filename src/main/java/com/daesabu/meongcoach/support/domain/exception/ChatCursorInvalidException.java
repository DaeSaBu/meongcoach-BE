package com.daesabu.meongcoach.support.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class ChatCursorInvalidException extends DomainException {
	public ChatCursorInvalidException() {
		super(SupportErrorCode.SUPPORT_CHAT_CURSOR_INVALID);
	}
}
