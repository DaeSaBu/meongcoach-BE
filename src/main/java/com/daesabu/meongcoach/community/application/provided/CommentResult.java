package com.daesabu.meongcoach.community.application.provided;

import com.daesabu.meongcoach.community.domain.CardComment;
import java.time.LocalDateTime;

/**
 * 댓글 한 건의 읽기 모델. 댓글과 답글이 같은 모양을 쓰며, 답글은 parentId에 답한 대상을 담는다.
 */
public record CommentResult(Long id, Long parentId, Long authorId, String authorNickname, String content,
                            LocalDateTime createdAt, long replyCount, long likeCount, boolean likedByMe) {

	// 방금 작성한 댓글은 답글도 좋아요도 없다
	public static CommentResult of(CardComment comment, String authorNickname) {
		return new CommentResult(comment.getId(), comment.getParentId(), comment.getAuthorId(), authorNickname,
				comment.getContent(), comment.getCreatedAt(), 0, 0, false);
	}
}
