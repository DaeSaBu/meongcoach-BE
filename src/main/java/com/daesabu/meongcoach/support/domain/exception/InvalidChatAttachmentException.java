package com.daesabu.meongcoach.support.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidChatAttachmentException extends DomainException {
	public InvalidChatAttachmentException() {
		super(SupportErrorCode.SUPPORT_CHAT_ATTACHMENT_INVALID);
	}
}
