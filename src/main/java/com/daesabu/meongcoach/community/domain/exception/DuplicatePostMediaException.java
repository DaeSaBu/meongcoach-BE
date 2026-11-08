package com.daesabu.meongcoach.community.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class DuplicatePostMediaException extends DomainException {

	public DuplicatePostMediaException() {
		super(CommunityErrorCode.COMMUNITY_POST_MEDIA_DUPLICATED);
	}
}
