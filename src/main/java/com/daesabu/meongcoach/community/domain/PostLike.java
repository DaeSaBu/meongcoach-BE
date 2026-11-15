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
@Table(name = "post_likes", uniqueConstraints = @UniqueConstraint(name = "uk_post_likes_target_user", columnNames = {"post_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(nullable = false)
	private Long postId;
	@Column(nullable = false)
	private Long userId;
	private LocalDateTime deletedAt;

	private PostLike(Long postId, Long userId) {
		this.postId = postId;
		this.userId = userId;
	}

	public static PostLike register(Long postId, Long userId) {
		return new PostLike(postId, userId);
	}

	public void delete() {
		if (deletedAt == null) {
			deletedAt = LocalDateTime.now();
		}
	}

	public void restore() {
		deletedAt = null;
	}
}
