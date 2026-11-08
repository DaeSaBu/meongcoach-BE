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
		CommentMediaList mediaList = new CommentMediaList();
		for (int index = 0; index < commands.size(); index++) {
			mediaList.items.add(CommentMedia.create(commands.get(index), index));
		}
		return mediaList;
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
