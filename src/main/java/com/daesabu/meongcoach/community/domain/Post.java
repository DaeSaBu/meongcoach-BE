package com.daesabu.meongcoach.community.domain;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.ofNullable;

import com.daesabu.meongcoach.community.domain.exception.PostMediaCountExceededException;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "post_id", nullable = false, updatable = false)
	@OrderBy("sortOrder ASC")
	private List<PostMedia> media = new ArrayList<>();

	public static Post register(Long userId, PostRegisterCommand command) {
		List<PostMediaCommand> media = requireNonNullElse(command.media(), List.of());
		validateMediaCount(media);

		Post post = new Post();
		post.userId = requireNonNull(userId);
		post.content = new PostContent(command.content());
		post.mention = ofNullable(command.mention())
				.map(PostMention::create)
				.orElse(null);
		post.media = createMedia(media);
		return post;
	}

	public List<PostMedia> getMedia() {
		return Collections.unmodifiableList(media);
	}

	private static void validateMediaCount(List<PostMediaCommand> media) {
		Arrays.stream(PostMediaType.values())
				.filter(type -> countOf(media, type) > type.getMaxCount())
				.findFirst()
				.ifPresent(type -> {
					throw new PostMediaCountExceededException(type);
				});
	}

	private static long countOf(List<PostMediaCommand> media, PostMediaType type) {
		return media.stream()
				.filter(command -> command.type() == type)
				.count();
	}

	private static List<PostMedia> createMedia(List<PostMediaCommand> media) {
		return IntStream.range(0, media.size())
				.mapToObj(index -> PostMedia.create(media.get(index), index))
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
