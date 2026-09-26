package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.domain.comment.CardComment;
import java.time.LocalDateTime;

public record ReplyResult(Long id, String authorNickname, String content, LocalDateTime createdAt) {

	public static ReplyResult from(CardComment reply, String nickname) {
		return new ReplyResult(reply.getId(), nickname, reply.getContent(), reply.getCreatedAt());
	}
}
