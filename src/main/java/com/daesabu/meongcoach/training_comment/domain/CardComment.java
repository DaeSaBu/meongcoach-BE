package com.daesabu.meongcoach.cardcomment.domain;

import com.daesabu.meongcoach.cardcomment.domain.exception.InvalidCommentContentException;
import com.daesabu.meongcoach.shared.domain.BaseEntity;
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

/**
 * 교육 카드의 댓글. 답글도 같은 엔티티이며 rootId가 있으면 답글이다.
 * 카드와 작성자는 다른 모듈의 엔티티라 연관 없이 ID로만 참조한다.
 */
@Getter
@Entity
@Table(name = "card_comments", indexes = {
		@Index(name = "idx_card_comments_card_id_id", columnList = "card_id, id"),
		@Index(name = "idx_card_comments_root_id_id", columnList = "root_id, id")
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
	private Long authorId;

	// 스레드의 최상위 댓글. 최상위 댓글 자신은 null
	private Long rootId;

	// 실제로 답한 댓글. 최상위 댓글은 null이고, 최상위 댓글에 단 답글은 rootId와 같다
	private Long parentId;

	@Column(nullable = false, length = CONTENT_MAX_LENGTH)
	private String content;

	private CardComment(Long cardId, Long authorId, Long rootId, Long parentId, String content) {
		this.cardId = cardId;
		this.authorId = authorId;
		this.rootId = rootId;
		this.parentId = parentId;
		this.content = normalizeContent(content);
	}

	public static CardComment create(CardCommentCreateCommand command) {
		return new CardComment(command.cardId(), command.authorId(), null, null, command.content());
	}

	// 답글의 답글은 같은 스레드에 붙이고 답한 대상만 parentId로 남긴다. 스레드는 한 단계로 평평하다
	public static CardComment reply(CardComment target, Long authorId, String content) {
		return new CardComment(target.cardId, authorId, target.threadRootId(), target.id, content);
	}

	public boolean isReply() {
		return rootId != null;
	}

	// 자신이 속한 스레드의 최상위 댓글 ID. 최상위 댓글은 자기 자신이다
	private Long threadRootId() {
		if (rootId != null) {
			return rootId;
		}
		return id;
	}

	private static String normalizeContent(String content) {
		if (content == null) {
			throw new InvalidCommentContentException();
		}
		String normalized = content.trim();
		if (normalized.isEmpty() || normalized.length() > CONTENT_MAX_LENGTH) {
			throw new InvalidCommentContentException();
		}
		return normalized;
	}
}
