package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.application.provided.dto.MentionTargetResult;
import com.daesabu.meongcoach.community.application.provided.dto.PostResult;
import com.daesabu.meongcoach.community.domain.MentionTargetType;
import com.daesabu.meongcoach.community.domain.Post;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public record PostResponse(Long postId, String content, AuthorResponse author, MentionResponse mention,
		List<PostMediaResponse> media, long commentCount, LocalDateTime createdAt) {

	public static PostResponse from(PostResult result) {
		Post post = result.post();
		AuthorResponse author = Optional.ofNullable(result.author())
				.map(AuthorResponse::from)
				.orElse(null);
		MentionResponse mention = Optional.ofNullable(result.mentionTarget())
				.map(MentionResponse::from)
				.orElse(null);
		List<PostMediaResponse> media = post.getMedia().stream()
				.map(PostMediaResponse::from)
				.toList();
		return new PostResponse(post.getId(), post.getContent().value(), author, mention, media,
				result.commentCount(), post.getCreatedAt());
	}

	public record MentionResponse(MentionTargetType targetType, Long targetId, String title) {

		public static MentionResponse from(MentionTargetResult result) {
			return new MentionResponse(result.targetType(), result.targetId(), result.title());
		}
	}
}
