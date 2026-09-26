package com.daesabu.meongcoach.training.domain.comment;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import com.daesabu.meongcoach.training.domain.exception.InvalidCardCommentContentException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "card_comments", indexes = {
		@Index(name = "idx_card_comments_card_id_id", columnList = "card_id, id"),
		@Index(name = "idx_card_comments_parent_id_id", columnList = "parent_id, id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardComment extends BaseEntity {

	public static final int CONTENT_MAX_LENGTH = 500;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long cardId;

	@Column(nullable = false)
	private Long userId;

	@Column
	private Long parentId;

	@Column(nullable = false, length = CONTENT_MAX_LENGTH)
	private String content;

	private CardComment(Long cardId, Long userId, Long parentId, String content) {
		this.cardId = cardId;
		this.userId = userId;
		this.parentId = parentId;
		this.content = normalizeContent(content);
	}

	public static CardComment create(Long cardId, Long userId, String content) {
		return new CardComment(cardId, userId, null, content);
	}

	public static CardComment createReply(CardComment target, Long userId, String content) {
		Long parentId = target.id;
		if (target.parentId != null) {
			parentId = target.parentId;
		}
		return new CardComment(target.cardId, userId, parentId, content);
	}

	private static String normalizeContent(String content) {
		if (content == null) {
			throw new InvalidCardCommentContentException();
		}
		String normalized = content.trim();
		if (normalized.isEmpty() || normalized.length() > CONTENT_MAX_LENGTH) {
			throw new InvalidCardCommentContentException();
		}
		return normalized;
	}
}
