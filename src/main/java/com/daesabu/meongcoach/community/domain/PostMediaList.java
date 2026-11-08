package com.daesabu.meongcoach.community.domain;

import com.daesabu.meongcoach.community.domain.exception.DuplicatePostMediaException;
import com.daesabu.meongcoach.community.domain.exception.PostMediaCountExceededException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PostMediaList {

	private static final int MAX_IMAGE_COUNT = 3;
	private static final int MAX_VIDEO_COUNT = 2;

	private final List<PostMedia> items;

	PostMediaList(List<PostMedia> items) {
		this.items = List.copyOf(items);
	}

	List<PostMedia> active() {
		return items.stream()
				.filter(PostMedia::isActive)
				.sorted(Comparator.comparingInt(PostMedia::getSortOrder))
				.toList();
	}

	List<PostMedia> update(List<PostMediaCommand> commands) {
		validate(commands);
		List<PostMedia> activeMedia = active();
		deleteNotIn(commands, activeMedia);
		return reuseOrCreate(commands, activeMedia);
	}

	private void deleteNotIn(List<PostMediaCommand> commands, List<PostMedia> activeMedia) {
		activeMedia.stream()
				.filter(existing -> commands.stream().noneMatch(existing::matches))
				.forEach(PostMedia::delete);
	}

	private List<PostMedia> reuseOrCreate(List<PostMediaCommand> commands, List<PostMedia> activeMedia) {
		List<PostMedia> created = new ArrayList<>();
		for (int index = 0; index < commands.size(); index++) {
			PostMediaCommand command = commands.get(index);
			PostMedia existing = activeMedia.stream()
					.filter(media -> media.matches(command))
					.findFirst()
					.orElse(null);
			if (existing != null) {
				existing.moveTo(index);
				continue;
			}
			created.add(PostMedia.create(command, index));
		}
		return created;
	}

	private static void validate(List<PostMediaCommand> commands) {
		validateCount(commands, PostMediaType.IMAGE, MAX_IMAGE_COUNT);
		validateCount(commands, PostMediaType.VIDEO, MAX_VIDEO_COUNT);
		if (commands.stream().distinct().count() != commands.size()) {
			throw new DuplicatePostMediaException();
		}
	}

	private static void validateCount(List<PostMediaCommand> commands, PostMediaType type, int maxCount) {
		long count = commands.stream()
				.filter(command -> command.type() == type)
				.count();
		if (count > maxCount) {
			throw new PostMediaCountExceededException(type, maxCount);
		}
	}
}
