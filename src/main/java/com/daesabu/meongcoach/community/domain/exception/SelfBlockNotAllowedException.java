package com.daesabu.meongcoach.community.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class SelfBlockNotAllowedException extends DomainException {

	public SelfBlockNotAllowedException() {
		super(CommunityErrorCode.COMMUNITY_SELF_BLOCK_NOT_ALLOWED);
	}
}
