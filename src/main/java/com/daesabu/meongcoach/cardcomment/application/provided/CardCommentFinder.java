package com.daesabu.meongcoach.cardcomment.application.provided;

import java.util.List;
import java.util.Set;

public interface CardCommentFinder {

	/**
	 * 카드의 최상위 댓글을 최신순으로 한 페이지(20건) 조회한다. cursor는 직전 페이지의 nextCursor이며 첫 페이지는 null이다.
	 * totalCount는 답글을 포함한 카드 전체 댓글 수다.
	 */
	CommentPageResult findComments(Long userId, Long cardId, Long cursor);

	/**
	 * 최상위 댓글의 답글을 오래된 순으로 한 페이지(20건) 조회한다. 답글 ID를 넘기면 댓글을 찾을 수 없는 것으로 본다.
	 * totalCount는 스레드의 답글 수다.
	 */
	CommentPageResult findReplies(Long userId, Long commentId, Long cursor);

	/**
	 * 카드별 댓글 수를 답글까지 포함해 조회한다. 댓글이 없는 카드는 결과에 없다.
	 */
	List<CardCommentCountResult> countComments(Set<Long> cardIds);
}
