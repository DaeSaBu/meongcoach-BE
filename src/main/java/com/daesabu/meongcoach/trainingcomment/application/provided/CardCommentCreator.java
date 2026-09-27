package com.daesabu.meongcoach.trainingcomment.application.provided;

import com.daesabu.meongcoach.trainingcomment.application.provided.dto.CommentCreateRequest;

public interface CardCommentCreator {

	CommentResult createComment(Long userId, Long cardId, CommentCreateRequest request);

	/**
	 * 답글을 작성한다. 대상이 답글이면 그 답글의 최상위 댓글에 붙인다.
	 */
	CommentResult createReply(Long userId, Long commentId, CommentCreateRequest request);
}
