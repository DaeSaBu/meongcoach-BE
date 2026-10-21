package com.daesabu.meongcoach.community.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class PostNotFoundException extends DomainException {

	public PostNotFoundException(Long postId) {
		super(CommunityErrorCode.COMMUNITY_POST_NOT_FOUND, "id가 " + postId + "인 게시글을 찾을 수 없습니다.");
	}
}
