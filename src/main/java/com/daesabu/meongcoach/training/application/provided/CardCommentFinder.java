package com.daesabu.meongcoach.training.application.provided;

import java.util.List;

public interface CardCommentFinder {

	List<CommentResult> findComments(Long cardId);

	long countComments(Long cardId);

	List<ReplyResult> findReplies(Long parentId);
}
