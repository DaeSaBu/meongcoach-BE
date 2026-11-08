package com.daesabu.meongcoach.community.domain;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.ofNullable;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Embedded
	private PostContent content;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "mention_id", unique = true)
	private PostMention mention;

	@Embedded
	private PostMediaList media = new PostMediaList();

	@Column(nullable = false)
	private int commentCount;

	private LocalDateTime deletedAt;

	public static Post register(Long userId, PostRegisterCommand command) {
		Post post = new Post();
		post.userId = requireNonNull(userId);
		post.content = new PostContent(command.content());
		post.mention = createMention(command.mention());
		post.media = PostMediaList.of(requireNonNullElse(command.media(), List.of()));
		return post;
	}

	public void update(PostUpdateCommand command) {
		this.media.update(requireNonNullElse(command.media(), List.of()));
		this.content = new PostContent(command.content());
		this.mention = createMention(command.mention());
	}

	public void delete() {
		this.deletedAt = LocalDateTime.now();
	}

	public void increaseCommentCount() {
		this.commentCount++;
	}

	public void decreaseCommentCount(int count) {
		this.commentCount = Math.max(0, this.commentCount - count);
	}

	public List<PostMedia> getMedia() {
		return media.active();
	}

	private static PostMention createMention(MentionCommand mention) {
		return ofNullable(mention)
				.map(PostMention::create)
				.orElse(null);
	}

}
