package com.daesabu.meongcoach.community.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "comment_likes", uniqueConstraints = @UniqueConstraint(name = "uk_comment_likes_target_user", columnNames = {"comment_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLike extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(nullable = false)
	private Long commentId;
	@Column(nullable = false)
	private Long userId;
	private LocalDateTime deletedAt;

	private CommentLike(Long commentId, Long userId) {
		this.commentId = commentId;
		this.userId = userId;
	}

	public static CommentLike register(Long commentId, Long userId) {
		return new CommentLike(commentId, userId);
	}

	public void unlike() {
		if (deletedAt == null) {
			deletedAt = LocalDateTime.now();
		}
	}

	public void relike() {
		deletedAt = null;
	}
}
