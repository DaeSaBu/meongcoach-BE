package com.daesabu.meongcoach.community.domain;

import com.daesabu.meongcoach.community.domain.exception.PostMediaCountExceededException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostMediaList {

	@OneToMany(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "post_id", nullable = false, updatable = false)
	@OrderBy("sortOrder ASC")
	private List<PostMedia> items = new ArrayList<>();

	static PostMediaList of(List<PostMediaCommand> commands) {
		validateCount(commands);
		PostMediaList list = new PostMediaList();
		list.items = IntStream.range(0, commands.size())
				.mapToObj(index -> PostMedia.create(commands.get(index), index))
				.collect(Collectors.toCollection(ArrayList::new));
		return list;
	}

	List<PostMedia> active() {
		return items.stream()
				.filter(PostMedia::isActive)
				.sorted(Comparator.comparingInt(PostMedia::getSortOrder))
				.toList();
	}

	void update(List<PostMediaCommand> commands) {
		validateCount(commands);
		deleteNotIn(commands);
		IntStream.range(0, commands.size())
				.forEach(index -> place(commands.get(index), index));
	}

	private void deleteNotIn(List<PostMediaCommand> commands) {
		active().stream()
				.filter(existing -> commands.stream().noneMatch(existing::matches))
				.forEach(PostMedia::delete);
	}

	private void place(PostMediaCommand command, int sortOrder) {
		active().stream()
				.filter(existing -> existing.matches(command))
				.findFirst()
				.ifPresentOrElse(existing -> existing.moveTo(sortOrder), () -> items.add(PostMedia.create(command, sortOrder)));
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
