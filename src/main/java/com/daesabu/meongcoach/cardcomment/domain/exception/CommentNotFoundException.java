package com.daesabu.meongcoach.cardcomment.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class CommentNotFoundException extends DomainException {

	public CommentNotFoundException(Long commentId) {
		super(CommentErrorCode.COMMENT_NOT_FOUND, "id가 " + commentId + "인 댓글을 찾을 수 없습니다.");
	}
}
