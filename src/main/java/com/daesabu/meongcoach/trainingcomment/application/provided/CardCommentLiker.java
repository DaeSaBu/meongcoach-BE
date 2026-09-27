package com.daesabu.meongcoach.trainingcomment.application.provided;

/**
 * 댓글 좋아요 능력. 추가와 취소 모두 멱등이라 같은 요청을 반복해도 상태가 같다.
 */
public interface CardCommentLiker {

	LikeStateResult like(Long userId, Long commentId);

	LikeStateResult unlike(Long userId, Long commentId);
}
