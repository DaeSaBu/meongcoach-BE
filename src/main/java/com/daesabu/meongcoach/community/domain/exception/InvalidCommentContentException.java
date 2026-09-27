package com.daesabu.meongcoach.community.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidCommentContentException extends DomainException {

	public InvalidCommentContentException() {
		super(CommentErrorCode.COMMENT_INVALID_CONTENT);
	}
}
