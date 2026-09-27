package com.daesabu.meongcoach.community.application.provided;

import com.daesabu.meongcoach.community.application.provided.dto.CommentCreateRequest;

public interface CardCommentCreator {

	CommentResult createComment(Long userId, Long cardId, CommentCreateRequest request);

	/**
	 * 답글을 작성한다. 대상이 답글이면 같은 스레드에 붙이고 답한 대상만 parentId로 남긴다.
	 */
	CommentResult createReply(Long userId, Long commentId, CommentCreateRequest request);
}
