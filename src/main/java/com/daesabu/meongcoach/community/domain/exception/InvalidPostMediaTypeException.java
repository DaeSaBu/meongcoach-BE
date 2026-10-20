package com.daesabu.meongcoach.community.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidPostMediaTypeException extends DomainException {

	public InvalidPostMediaTypeException(String code) {
		super(CommunityErrorCode.COMMUNITY_POST_MEDIA_TYPE_INVALID, "지원하지 않는 미디어 종류입니다: " + code);
	}
}
