package com.daesabu.meongcoach.community.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.community.domain.exception.CommentMediaCountExceededException;
import java.util.List;

public class CommentMediaList {

	private static final int MAX_COUNT = 1;

	private final List<CommentMedia> items;

	CommentMediaList(List<CommentMedia> items) {
		this.items = requireNonNull(items);
	}

	void register(List<PostMediaCommand> commands) {
		validateCount(commands);
		for (PostMediaCommand command : commands) {
			items.add(CommentMedia.create(command, items.size()));
		}
	}

	private void validateCount(List<PostMediaCommand> commands) {
		if (items.size() + commands.size() > MAX_COUNT) {
			throw new CommentMediaCountExceededException();
		}
	}
}
