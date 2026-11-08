package com.daesabu.meongcoach.community.domain;

import com.daesabu.meongcoach.community.domain.exception.DuplicatePostMediaException;
import com.daesabu.meongcoach.community.domain.exception.PostMediaCountExceededException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostMediaList {

	private static final Map<PostMediaType, Integer> MAX_COUNTS = Map.of(PostMediaType.IMAGE, 3, PostMediaType.VIDEO, 2);

	@OneToMany(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "post_id", nullable = false, updatable = false)
	@OrderBy("sortOrder ASC")
	private List<PostMedia> items = new ArrayList<>();

	static PostMediaList of(List<PostMediaCommand> commands) {
		PostMediaList mediaList = new PostMediaList();
		mediaList.update(commands);
		return mediaList;
	}

	List<PostMedia> active() {
		return items.stream()
				.filter(PostMedia::isActive)
				.sorted(Comparator.comparingInt(PostMedia::getSortOrder))
				.toList();
	}

	void update(List<PostMediaCommand> commands) {
		validate(commands);
		List<PostMedia> activeMedia = active();
		deleteNotIn(commands, activeMedia);
		for (int index = 0; index < commands.size(); index++) {
			reuseOrCreate(commands.get(index), index, activeMedia);
		}
	}

	private void deleteNotIn(List<PostMediaCommand> commands, List<PostMedia> activeMedia) {
		activeMedia.stream()
				.filter(existing -> commands.stream().noneMatch(existing::matches))
				.forEach(PostMedia::delete);
	}

	private void reuseOrCreate(PostMediaCommand command, int sortOrder, List<PostMedia> activeMedia) {
		PostMedia existing = activeMedia.stream()
				.filter(media -> media.matches(command))
				.findFirst()
				.orElse(null);
		if (existing != null) {
			existing.moveTo(sortOrder);
			return;
		}
		items.add(PostMedia.create(command, sortOrder));
	}

	private static void validate(List<PostMediaCommand> commands) {
		validateCount(commands);
		if (commands.stream().distinct().count() != commands.size()) {
			throw new DuplicatePostMediaException();
		}
	}

	private static void validateCount(List<PostMediaCommand> commands) {
		MAX_COUNTS.forEach((type, maxCount) -> {
			if (countOf(commands, type) > maxCount) {
				throw new PostMediaCountExceededException(type, maxCount);
			}
		});
	}

	private static long countOf(List<PostMediaCommand> commands, PostMediaType type) {
		return commands.stream()
				.filter(command -> command.type() == type)
				.count();
	}
}
