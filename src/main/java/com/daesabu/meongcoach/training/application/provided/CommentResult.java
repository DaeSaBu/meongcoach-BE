package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.domain.comment.CardComment;
import java.time.LocalDateTime;

public record CommentResult(Long id, String authorNickname, String content, LocalDateTime createdAt,
	                            long replyCount) {

	public static CommentResult from(CardComment comment, String nickname, long replyCount) {
		return new CommentResult(comment.getId(), nickname, comment.getContent(), comment.getCreatedAt(), replyCount);
	}
}
