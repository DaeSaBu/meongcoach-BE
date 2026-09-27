package com.daesabu.meongcoach.community.domain.exception;

import com.daesabu.meongcoach.community.domain.CommentTarget;
import com.daesabu.meongcoach.shared.exception.DomainException;

public class CommentTargetNotFoundException extends DomainException {

	public CommentTargetNotFoundException(CommentTarget target) {
		super(CommentErrorCode.COMMENT_TARGET_NOT_FOUND,
				"종류가 " + target.type() + "이고 id가 " + target.id() + "인 댓글 대상을 찾을 수 없습니다.");
	}
}
