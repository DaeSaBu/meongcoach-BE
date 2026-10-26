package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.domain.MentionTargetType;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.PostMention;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public record PostUpdateResponse(Long postId, Long userId, String content, MentionResponse mention,
		List<PostMediaResponse> media, LocalDateTime createdAt) {

	public static PostUpdateResponse from(Post post) {
		MentionResponse mention = Optional.ofNullable(post.getMention())
				.map(MentionResponse::from)
				.orElse(null);
		List<PostMediaResponse> media = post.getMedia().stream()
				.map(PostMediaResponse::from)
				.toList();
		return new PostUpdateResponse(post.getId(), post.getUserId(), post.getContent().value(), mention, media,
				post.getCreatedAt());
	}

	public record MentionResponse(MentionTargetType targetType, Long targetId) {

		public static MentionResponse from(PostMention mention) {
			return new MentionResponse(mention.getTargetType(), mention.getTargetId());
		}
	}
}
