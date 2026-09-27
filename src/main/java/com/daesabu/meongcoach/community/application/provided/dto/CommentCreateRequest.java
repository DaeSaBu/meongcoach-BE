package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.community.domain.CommentCreateCommand;
import com.daesabu.meongcoach.community.domain.CommentTarget;

// 본문 검증은 Comment가 도메인 규칙으로 수행하므로 제약 어노테이션을 두지 않는다
public record CommentCreateRequest(String content) {

	public CommentCreateCommand toCommand(CommentTarget target, Long authorId) {
		return new CommentCreateCommand(target, authorId, content);
	}
}
