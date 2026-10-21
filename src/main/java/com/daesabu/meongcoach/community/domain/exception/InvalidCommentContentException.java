package com.daesabu.meongcoach.community.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidCommentContentException extends DomainException {

	public InvalidCommentContentException(String detail) {
		super(CommunityErrorCode.COMMUNITY_COMMENT_CONTENT_INVALID, detail);
	}
}
