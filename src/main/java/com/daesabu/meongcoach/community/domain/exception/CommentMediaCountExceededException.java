package com.daesabu.meongcoach.community.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class CommentMediaCountExceededException extends DomainException {

	public CommentMediaCountExceededException() {
		super(CommunityErrorCode.COMMUNITY_COMMENT_MEDIA_COUNT_EXCEEDED, "댓글에는 사진이나 영상을 1개까지 첨부할 수 있습니다.");
	}
}
