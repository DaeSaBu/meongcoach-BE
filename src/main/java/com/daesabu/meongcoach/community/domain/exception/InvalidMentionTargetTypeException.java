package com.daesabu.meongcoach.community.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidMentionTargetTypeException extends DomainException {

	public InvalidMentionTargetTypeException(String code) {
		super(CommunityErrorCode.COMMUNITY_MENTION_TARGET_TYPE_INVALID, "지원하지 않는 멘션 대상 종류입니다: " + code);
	}
}
