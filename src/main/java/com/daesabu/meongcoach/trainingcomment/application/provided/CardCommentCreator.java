package com.daesabu.meongcoach.trainingcomment.application.provided;

import com.daesabu.meongcoach.trainingcomment.application.provided.dto.CommentCreateRequest;
import jakarta.validation.Valid;

public interface CardCommentCreator {

	CommentResult createComment(Long userId, Long cardId, @Valid CommentCreateRequest request);

	/**
	 * 답글을 작성한다. 대상이 답글이면 그 답글의 최상위 댓글에 붙인다.
	 */
	CommentResult createReply(Long userId, Long commentId, @Valid CommentCreateRequest request);
}
