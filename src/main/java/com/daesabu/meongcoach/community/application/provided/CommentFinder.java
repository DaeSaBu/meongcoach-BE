package com.daesabu.meongcoach.community.application.provided;

import com.daesabu.meongcoach.community.application.provided.dto.CommentListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.CommentListResult;
import com.daesabu.meongcoach.community.application.provided.dto.ReplyListResult;
import jakarta.validation.Valid;

/**
 * 댓글 조회 능력.
 */
public interface CommentFinder {

	/**
	 * 게시글의 삭제되지 않은 루트 댓글을 ID 오름차순으로 size개까지 반환한다. cursor가 있으면 그보다 큰 ID의 댓글만 반환한다.
	 * 다음 페이지가 있으면 nextCursor에 마지막 댓글 ID가 담기고, 없으면 null이다.
	 * 각 댓글에는 작성자 프로필 요약이 함께 담기며, 프로필이 없는 작성자는 author가 null이다.
	 * 각 댓글의 replyCount는 삭제되지 않은 답글 수이며, 답글이 없으면 0이다.
	 * 게시글이 없거나 삭제됐으면 PostNotFoundException을 던진다.
	 */
	CommentListResult findComments(Long postId, @Valid CommentListRequest request);

	/**
	 * 루트 댓글의 답글을 ID 오름차순으로 size개까지 반환한다. cursor가 있으면 그보다 큰 ID의 답글만 반환한다.
	 * 다음 페이지가 있으면 nextCursor에 마지막 답글 ID가 담기고, 없으면 null이다.
	 * 각 답글에는 작성자 프로필 요약이 함께 담기며, 프로필이 없는 작성자는 author가 null이다. 삭제된 답글은 담기지 않는다.
	 * 루트 댓글이 삭제됐어도 답글을 반환한다. 댓글이 없거나 루트 댓글이 아니면 CommentNotFoundException을 던진다.
	 */
	ReplyListResult findReplies(Long commentId, @Valid CommentListRequest request);
}
