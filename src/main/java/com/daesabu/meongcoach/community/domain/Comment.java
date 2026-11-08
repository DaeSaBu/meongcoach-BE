package com.daesabu.meongcoach.community.domain;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

	@OneToMany(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "comment_id", nullable = false, updatable = false)
	@OrderBy("sortOrder ASC")
	private List<CommentMedia> media = new ArrayList<>();

	private LocalDateTime deletedAt;

	public static Comment register(Post post, Long userId, String content) {
		return register(post, userId, content, List.of());
	}

	public static Comment register(Post post, Long userId, String content, List<PostMediaCommand> media) {
		List<PostMediaCommand> commands = requireNonNullElse(media, List.of());
		PostMediaType.validateCount(commands);

		Comment comment = new Comment();
		comment.post = requireNonNull(post);
		comment.userId = requireNonNull(userId);
		comment.content = new CommentContent(content);
		comment.media = createMedia(commands);
		return comment;
	}

	public void update(String content) {
		this.content = new CommentContent(content);
	}


	public void delete() {
		this.deletedAt = LocalDateTime.now();
	}

	public List<CommentMedia> getMedia() {
		return Collections.unmodifiableList(media);
	}

	private static List<CommentMedia> createMedia(List<PostMediaCommand> media) {
		return IntStream.range(0, media.size())
				.mapToObj(index -> CommentMedia.create(media.get(index), index))
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
