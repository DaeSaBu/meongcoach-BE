package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.community.domain.Post;

public record PostResult(Post post, AuthorResult author, MentionTargetResult mentionTarget,
		long commentCount) {
}
