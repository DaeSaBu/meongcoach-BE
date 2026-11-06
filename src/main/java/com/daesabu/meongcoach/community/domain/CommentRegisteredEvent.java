package com.daesabu.meongcoach.community.domain;

import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public record CommentRegisteredEvent(
		Long commentId,
		Long postId,
		Long postAuthorId,
		Long commenterId,
		Long rootCommentAuthorId,
		String content
) {

	public static CommentRegisteredEvent from(Comment comment, Comment root) {
		Post post = comment.getPost();
		Long rootCommentAuthorId = ofNullable(root).map(Comment::getUserId).orElse(null);
		return new CommentRegisteredEvent(comment.getId(), post.getId(), post.getUserId(), comment.getUserId(),
				rootCommentAuthorId, comment.getContent().value());
	}

	public boolean isReply() {
		return rootCommentAuthorId != null;
	}

	public List<Long> recipientIds() {
		return Stream.of(postAuthorId, rootCommentAuthorId)
				.filter(Objects::nonNull)
				.filter(recipientId -> !recipientId.equals(commenterId))
				.distinct()
				.toList();
	}
}
