package com.daesabu.meongcoach.support.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class ChatAccessDeniedException extends DomainException {
	public ChatAccessDeniedException() {
		super(SupportErrorCode.SUPPORT_CHAT_ACCESS_DENIED);
	}
}
