package com.daesabu.meongcoach.training.application.provided;

public interface CardCommentLikeUpdater {

	LikeStateResult like(Long userId, Long commentId);

	LikeStateResult unlike(Long userId, Long commentId);
}
