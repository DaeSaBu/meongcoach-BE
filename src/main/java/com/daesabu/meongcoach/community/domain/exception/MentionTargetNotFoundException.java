package com.daesabu.meongcoach.community.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class MentionTargetNotFoundException extends DomainException {

	public MentionTargetNotFoundException(Long targetId) {
		super(CommunityErrorCode.COMMUNITY_MENTION_TARGET_NOT_FOUND, "id가 " + targetId + "인 멘션 대상을 찾을 수 없습니다.");
	}
}
