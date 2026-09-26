package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.application.provided.dto.CommentCreateRequest;
import jakarta.validation.Valid;

public interface CardCommentCreator {

	CommentResult createComment(Long userId, Long cardId, @Valid CommentCreateRequest request);

	ReplyResult createReply(Long userId, Long commentId, @Valid CommentCreateRequest request);
}
