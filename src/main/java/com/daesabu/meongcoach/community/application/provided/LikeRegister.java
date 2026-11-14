package com.daesabu.meongcoach.community.application.provided;

import com.daesabu.meongcoach.community.domain.LikeSummary;

public interface LikeRegister {

	/**
	 * 게시글 좋아요를 추가하고 좋아요 수와 내 상태를 반환한다. 중복 요청은 같은 상태를 유지한다.
	 * 대상이 없거나 삭제됐으면 해당 도메인의 NotFoundException을 던진다.
	 */
	LikeSummary likePost(Long userId, Long postId);

	/**
	 * 게시글 좋아요를 취소하고 좋아요 수와 내 상태를 반환한다. 중복 요청은 같은 상태를 유지한다.
	 * 대상이 없거나 삭제됐으면 해당 도메인의 NotFoundException을 던진다.
	 */
	LikeSummary unlikePost(Long userId, Long postId);

	/**
	 * 댓글·답글 좋아요를 추가하고 좋아요 수와 내 상태를 반환한다. 중복 요청은 같은 상태를 유지한다.
	 * 대상이 없거나 삭제됐으면 해당 도메인의 NotFoundException을 던진다.
	 */
	LikeSummary likeComment(Long userId, Long commentId);

	/**
	 * 댓글·답글 좋아요를 취소하고 좋아요 수와 내 상태를 반환한다. 중복 요청은 같은 상태를 유지한다.
	 * 대상이 없거나 삭제됐으면 해당 도메인의 NotFoundException을 던진다.
	 */
	LikeSummary unlikeComment(Long userId, Long commentId);
}
