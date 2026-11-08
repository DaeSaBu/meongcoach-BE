package com.daesabu.meongcoach.community.domain;

import static java.util.Objects.requireNonNullElse;

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

	void register(List<PostMediaCommand> commands) {
		List<PostMediaCommand> requested = requireNonNullElse(commands, List.of());
		validateCount(requested);
		for (PostMediaCommand command : requested) {
			items.add(CommentMedia.create(command, items.size()));
		}
	}

	List<CommentMedia> list() {
		return Collections.unmodifiableList(items);
	}

	private void validateCount(List<PostMediaCommand> commands) {
		if (items.size() + commands.size() > MAX_COUNT) {
			throw new CommentMediaCountExceededException();
		}
	}
}
