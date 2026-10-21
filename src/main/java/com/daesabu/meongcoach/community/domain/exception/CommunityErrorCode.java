package com.daesabu.meongcoach.community.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum CommunityErrorCode implements ErrorCode {

	COMMUNITY_POST_CONTENT_INVALID(400, "게시글 본문이 올바르지 않습니다."),
	COMMUNITY_MENTION_TARGET_TYPE_INVALID(400, "지원하지 않는 멘션 대상 종류입니다."),
	COMMUNITY_MENTION_TARGET_NOT_FOUND(404, "멘션 대상을 찾을 수 없습니다."),
	COMMUNITY_COMMENT_CONTENT_INVALID(400, "댓글 본문이 올바르지 않습니다."),
	COMMUNITY_POST_NOT_FOUND(404, "게시글을 찾을 수 없습니다."),
	COMMUNITY_COMMENT_NOT_FOUND(404, "댓글을 찾을 수 없습니다."),
	COMMUNITY_POST_MEDIA_COUNT_EXCEEDED(400, "첨부할 수 있는 미디어 개수를 넘었습니다.");

	private final int status;
	private final String message;

	CommunityErrorCode(int status, String message) {
		this.status = status;
		this.message = message;
	}

	@Override
	public String code() {
		return name();
	}

	@Override
	public String message() {
		return message;
	}

	@Override
	public int status() {
		return status;
	}
}
