package com.daesabu.meongcoach.community.domain.exception;

import com.daesabu.meongcoach.community.domain.PostMediaType;
import com.daesabu.meongcoach.shared.exception.DomainException;

public class PostMediaCountExceededException extends DomainException {

	public PostMediaCountExceededException(PostMediaType type) {
		super(CommunityErrorCode.COMMUNITY_POST_MEDIA_COUNT_EXCEEDED,
				type.getDisplayName() + "은 최대 " + type.getMaxCount() + "개까지 첨부할 수 있습니다.");
	}
}
