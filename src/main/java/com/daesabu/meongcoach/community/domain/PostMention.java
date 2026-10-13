package com.daesabu.meongcoach.community.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "post_mentions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostMention extends BaseTimeEntity {

	@Id
	private Long postId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MentionTargetType targetType;

	@Column(nullable = false)
	private Long targetId;

	public static PostMention mention(Post post, MentionTargetType targetType, Long targetId) {
		PostMention mention = new PostMention();
		mention.post = requireNonNull(post);
		mention.targetType = requireNonNull(targetType);
		mention.targetId = requireNonNull(targetId);
		return mention;
	}

	public boolean isTarget(MentionTargetType type) {
		return this.targetType == type;
	}
}
