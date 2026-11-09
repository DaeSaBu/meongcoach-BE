package com.daesabu.meongcoach.community.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false, updatable = false)
	private Post post;

	@Column(nullable = false)
	private Long userId;

	@Embedded
	private CommentContent content;

	@Embedded
	private CommentMediaList media = new CommentMediaList();

	private LocalDateTime deletedAt;

	public static Comment register(Post post, Long userId, String content) {
		return register(post, userId, content, List.of());
	}

	public static Comment register(Post post, Long userId, String content, List<PostMediaCommand> media) {
		Comment comment = new Comment();
		comment.post = requireNonNull(post);
		comment.userId = requireNonNull(userId);
		comment.content = new CommentContent(content);
		comment.media.register(media);
		return comment;
	}

	public void update(String content) {
		this.content = new CommentContent(content);
	}

	public void delete() {
		this.deletedAt = LocalDateTime.now();
	}

	public List<CommentMedia> getMedia() {
		return media.list();
	}

}
