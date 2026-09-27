package com.daesabu.meongcoach.community.domain;

import com.daesabu.meongcoach.community.domain.exception.InvalidCommentContentException;
import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
 * 콘텐츠에 달리는 댓글. 답글도 같은 엔티티이며 rootId가 있으면 답글이다.
 * 대상과 작성자는 다른 모듈의 엔티티라 연관 없이 값으로만 참조한다.
 */
@Getter
@Entity
@Table(name = "comments", indexes = {
		@Index(name = "idx_comments_target_type_target_id_id", columnList = "target_type, target_id, id"),
		@Index(name = "idx_comments_root_id_id", columnList = "root_id, id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

	public static final int CONTENT_MAX_LENGTH = 500;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Embedded
	private CommentTarget target;

	@Column(nullable = false)
	private Long authorId;

	// 스레드의 최상위 댓글. 최상위 댓글 자신은 null
	private Long rootId;

	// 실제로 답한 댓글. 최상위 댓글은 null이고, 최상위 댓글에 단 답글은 rootId와 같다
	private Long parentId;

	@Column(nullable = false, length = CONTENT_MAX_LENGTH)
	private String content;

	private Comment(CommentTarget target, Long authorId, Long rootId, Long parentId, String content) {
		this.target = target;
		this.authorId = authorId;
		this.rootId = rootId;
		this.parentId = parentId;
		this.content = normalizeContent(content);
	}

	public static Comment create(CommentCreateCommand command) {
		return new Comment(command.target(), command.authorId(), null, null, command.content());
	}

	// 답글의 답글은 같은 스레드에 붙이고 답한 대상만 parentId로 남긴다. 스레드는 한 단계로 평평하다
	public static Comment reply(Comment parent, Long authorId, String content) {
		return new Comment(parent.target, authorId, parent.threadRootId(), parent.id, content);
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
