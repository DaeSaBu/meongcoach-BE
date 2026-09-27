package com.daesabu.meongcoach.community.application.provided;

/**
 * 카드 하나의 댓글 수. 답글을 포함한다.
 */
public record CardCommentCountResult(Long cardId, long totalCount) {
}
