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
		List<Long> participantIds,
		String content
) {

	public CommentRegisteredEvent {
		participantIds = List.copyOf(participantIds);
	}

	public static CommentRegisteredEvent from(Comment comment, Comment root, List<Long> participantIds) {
		Post post = comment.getPost();
		Long rootCommentAuthorId = ofNullable(root).map(Comment::getUserId).orElse(null);
		return new CommentRegisteredEvent(comment.getId(), post.getId(), post.getUserId(), comment.getUserId(),
				rootCommentAuthorId, participantIds, comment.getContent().value());
	}

	public boolean isReply() {
		return rootCommentAuthorId != null;
	}

	public List<Long> recipientIds() {
		return Stream.concat(Stream.of(postAuthorId, rootCommentAuthorId), participantIds.stream())
				.filter(Objects::nonNull)
				.filter(recipientId -> !recipientId.equals(commenterId))
				.distinct()
				.toList();
	}
}
