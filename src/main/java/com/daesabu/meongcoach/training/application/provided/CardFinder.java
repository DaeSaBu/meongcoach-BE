package com.daesabu.meongcoach.training.application.provided;

/**
 * 카드 존재 확인 능력. 카드에 귀속되는 다른 모듈(댓글)이 카드 ID가 유효한지 확인할 때 쓴다.
 */
public interface CardFinder {

	boolean existsCard(Long cardId);
}
