package com.daesabu.meongcoach.community.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidPostContentException extends DomainException {

	public InvalidPostContentException(String detail) {
		super(CommunityErrorCode.COMMUNITY_POST_CONTENT_INVALID, detail);
	}
}
