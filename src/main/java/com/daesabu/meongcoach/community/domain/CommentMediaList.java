package com.daesabu.meongcoach.community.domain;

import com.daesabu.meongcoach.community.domain.exception.PostMediaCountExceededException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentMediaList {

	@OneToMany(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "comment_id", nullable = false, updatable = false)
	@OrderBy("sortOrder ASC")
	private List<CommentMedia> items = new ArrayList<>();

	static CommentMediaList of(List<PostMediaCommand> commands) {
		validateCount(commands);
		CommentMediaList list = new CommentMediaList();
		list.items = IntStream.range(0, commands.size())
				.mapToObj(index -> CommentMedia.create(commands.get(index), index))
				.collect(Collectors.toCollection(ArrayList::new));
		return list;
	}

	List<CommentMedia> list() {
		return Collections.unmodifiableList(items);
	}

	private static void validateCount(List<PostMediaCommand> commands) {
		Arrays.stream(PostMediaType.values())
				.filter(type -> countOf(commands, type) > type.getMaxCount())
				.findFirst()
				.ifPresent(type -> {
					throw new PostMediaCountExceededException(type);
				});
	}

	private static long countOf(List<PostMediaCommand> commands, PostMediaType type) {
		return commands.stream()
				.filter(command -> command.type() == type)
				.count();
	}
}
