package com.daesabu.meongcoach.cardcomment.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum CommentErrorCode implements ErrorCode {

	COMMENT_NOT_FOUND(404, "댓글을 찾을 수 없습니다."),
	COMMENT_CARD_NOT_FOUND(404, "카드를 찾을 수 없습니다."),
	COMMENT_INVALID_CONTENT(400, "댓글 본문은 1~500자여야 합니다.");

	private final int status;
	private final String message;

	CommentErrorCode(int status, String message) {
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
