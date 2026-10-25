package com.daesabu.meongcoach.community.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "comment_closures",
		uniqueConstraints = @UniqueConstraint(columnNames = {"ancestor_id", "descendant_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentClosure extends BaseTimeEntity {

	private static final int SELF_DEPTH = 0;
	public static final int REPLY_DEPTH = 1;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ancestor_id", nullable = false, updatable = false)
	private Comment ancestor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "descendant_id", nullable = false, updatable = false)
	private Comment descendant;

	@Column(nullable = false, updatable = false)
	private int depth;

	public static CommentClosure self(Comment comment) {
		return connect(comment, comment, SELF_DEPTH);
	}

	public static CommentClosure reply(Comment root, Comment reply) {
		return connect(root, reply, REPLY_DEPTH);
	}

	private static CommentClosure connect(Comment ancestor, Comment descendant, int depth) {
		CommentClosure closure = new CommentClosure();
		closure.ancestor = requireNonNull(ancestor);
		closure.descendant = requireNonNull(descendant);
		closure.depth = depth;
		return closure;
	}
}
