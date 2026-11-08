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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

	@OneToMany(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "post_id", nullable = false, updatable = false)
	@OrderBy("sortOrder ASC")
	private List<PostMedia> media = new ArrayList<>();

	@Column(nullable = false)
	private int commentCount;

	private LocalDateTime deletedAt;

	public static Post register(Long userId, PostRegisterCommand command) {
		List<PostMediaCommand> media = requireNonNullElse(command.media(), List.of());
		PostMediaType.validateCount(media);

		Post post = new Post();
		post.userId = requireNonNull(userId);
		post.content = new PostContent(command.content());
		post.mention = createMention(command.mention());
		post.media = createMedia(media);
		return post;
	}

	public void update(PostUpdateCommand command) {
		List<PostMediaCommand> media = requireNonNullElse(command.media(), List.of());
		PostMediaType.validateCount(media);

		this.content = new PostContent(command.content());
		this.mention = createMention(command.mention());
		replaceMedia(media);
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
		return media.stream()
				.filter(PostMedia::isActive)
				.sorted(Comparator.comparingInt(PostMedia::getSortOrder))
				.toList();
	}

	private void replaceMedia(List<PostMediaCommand> commands) {
		deleteMediaNotIn(commands);
		IntStream.range(0, commands.size())
				.forEach(index -> placeMedia(commands.get(index), index));
	}

	private void deleteMediaNotIn(List<PostMediaCommand> commands) {
		getMedia().stream()
				.filter(existing -> commands.stream().noneMatch(existing::matches))
				.forEach(PostMedia::delete);
	}

	private void placeMedia(PostMediaCommand command, int sortOrder) {
		getMedia().stream()
				.filter(existing -> existing.matches(command))
				.findFirst()
				.ifPresentOrElse(existing -> existing.moveTo(sortOrder), () -> media.add(PostMedia.create(command, sortOrder)));
	}

	private static PostMention createMention(MentionCommand mention) {
		return ofNullable(mention)
				.map(PostMention::create)
				.orElse(null);
	}

	private static List<PostMedia> createMedia(List<PostMediaCommand> media) {
		return IntStream.range(0, media.size())
				.mapToObj(index -> PostMedia.create(media.get(index), index))
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
