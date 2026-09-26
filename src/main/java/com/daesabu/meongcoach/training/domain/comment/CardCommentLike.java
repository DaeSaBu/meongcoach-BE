package com.daesabu.meongcoach.training.domain.comment;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "card_comment_likes", indexes = {
		@Index(name = "idx_card_comment_likes_card_id_comment_id", columnList = "card_id, comment_id")
})
@IdClass(CardCommentLikeId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardCommentLike extends BaseTimeEntity {

	@Id
	@Column(nullable = false)
	private Long commentId;

	@Id
	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long cardId;
}
