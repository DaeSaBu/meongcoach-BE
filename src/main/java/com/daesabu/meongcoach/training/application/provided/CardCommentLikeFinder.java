package com.daesabu.meongcoach.training.application.provided;

import java.util.List;

public interface CardCommentLikeFinder {

	List<CommentLikeResult> findLikeStates(Long userId, Long cardId);
}
