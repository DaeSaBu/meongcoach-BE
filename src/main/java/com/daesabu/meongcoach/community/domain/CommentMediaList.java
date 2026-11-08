package com.daesabu.meongcoach.community.domain;

import com.daesabu.meongcoach.community.domain.exception.CommentMediaCountExceededException;
import java.util.ArrayList;
import java.util.List;

public class CommentMediaList {

	private static final int MAX_COUNT = 1;

	private final List<CommentMedia> items;

	CommentMediaList(List<CommentMedia> items) {
		this.items = List.copyOf(items);
	}

	List<CommentMedia> register(List<PostMediaCommand> commands) {
		validateCount(commands);
		List<CommentMedia> created = new ArrayList<>();
		for (int index = 0; index < commands.size(); index++) {
			created.add(CommentMedia.create(commands.get(index), items.size() + index));
		}
		return created;
	}

	private void validateCount(List<PostMediaCommand> commands) {
		if (items.size() + commands.size() > MAX_COUNT) {
			throw new CommentMediaCountExceededException();
		}
	}
}
