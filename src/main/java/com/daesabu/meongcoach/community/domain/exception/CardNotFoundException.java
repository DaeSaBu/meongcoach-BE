package com.daesabu.meongcoach.community.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class CardNotFoundException extends DomainException {

	public CardNotFoundException(Long cardId) {
		super(CommentErrorCode.COMMENT_CARD_NOT_FOUND, "id가 " + cardId + "인 카드를 찾을 수 없습니다.");
	}
}
