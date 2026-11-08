package com.daesabu.meongcoach.community.domain;

import com.daesabu.meongcoach.community.domain.exception.CommentMediaCountExceededException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentMediaList {

	private static final int MAX_COUNT = 1;

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
		if (commands.size() > MAX_COUNT) {
			throw new CommentMediaCountExceededException();
		}
	}
}
