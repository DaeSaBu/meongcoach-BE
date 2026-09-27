package com.daesabu.meongcoach.community.domain;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 댓글 좋아요. 댓글과 사용자의 조합이 곧 식별자라 같은 사용자가 같은 댓글을 두 번 좋아할 수 없다.
 */
@Getter
@Entity
@Table(name = "comment_likes")
@IdClass(CommentLikeId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLike extends BaseTimeEntity {

	@Id
	@Column(nullable = false)
	private Long commentId;

	@Id
	@Column(nullable = false)
	private Long userId;

	private CommentLike(Long commentId, Long userId) {
		this.commentId = commentId;
		this.userId = userId;
	}

	public static CommentLike create(Long commentId, Long userId) {
		return new CommentLike(commentId, userId);
	}
}
