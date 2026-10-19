package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.application.provided.dto.PostResult;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.training.application.provided.dto.CardSummaryResult;
import com.daesabu.meongcoach.user.application.provided.UserProfileSummaryResult;
import java.time.LocalDateTime;
import java.util.Optional;

public record PostResponse(Long postId, String content, AuthorResponse author, MentionedCardResponse mentionedCard,
		LocalDateTime createdAt) {

	public static PostResponse from(PostResult result) {
		Post post = result.post();
		AuthorResponse author = Optional.ofNullable(result.author())
				.map(AuthorResponse::from)
				.orElse(null);
		MentionedCardResponse mentionedCard = Optional.ofNullable(result.mentionedCard())
				.map(MentionedCardResponse::from)
				.orElse(null);
		return new PostResponse(post.getId(), post.getContent().value(), author, mentionedCard, post.getCreatedAt());
	}

	public record AuthorResponse(Long userId, String nickname, String profileImageUrl) {

		public static AuthorResponse from(UserProfileSummaryResult result) {
			return new AuthorResponse(result.userId(), result.nickname(), result.profileImageUrl());
		}
	}

	public record MentionedCardResponse(Long cardId, String title) {

		public static MentionedCardResponse from(CardSummaryResult result) {
			return new MentionedCardResponse(result.id(), result.title());
		}
	}
}
