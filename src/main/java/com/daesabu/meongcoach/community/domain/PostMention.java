package com.daesabu.meongcoach.community.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
		name = "post_mentions",
		uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "target_type", "target_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostMention extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MentionTargetType targetType;

	@Column(nullable = false)
	private Long targetId;

	PostMention(Post post, MentionCommand command) {
		this.post = requireNonNull(post);
		this.targetType = requireNonNull(command.targetType());
		this.targetId = requireNonNull(command.targetId());
	}

	public boolean isTarget(MentionTargetType type) {
		return this.targetType == type;
	}
}
