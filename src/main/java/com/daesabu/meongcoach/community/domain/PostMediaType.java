package com.daesabu.meongcoach.community.domain;

import com.daesabu.meongcoach.community.domain.exception.InvalidPostMediaTypeException;
import com.daesabu.meongcoach.media.domain.shared.MediaKind;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import com.daesabu.meongcoach.media.domain.shared.MediaVisibility;
import java.util.Arrays;
import lombok.Getter;

@Getter
public enum PostMediaType {
	IMAGE("사진", 3, MediaScope.of(MediaKind.IMAGE, MediaVisibility.PUBLIC, "community-post"), null),
	VIDEO("영상", 2, MediaScope.of(MediaKind.VIDEO, MediaVisibility.PUBLIC, "community-post"), 512L * 1024 * 1024);

	private final String displayName;
	private final int maxCount;
	private final MediaScope mediaScope;
	private final Long maxFileSizeBytes;

	PostMediaType(String displayName, int maxCount, MediaScope mediaScope, Long maxFileSizeBytes) {
		this.displayName = displayName;
		this.maxCount = maxCount;
		this.mediaScope = mediaScope;
		this.maxFileSizeBytes = maxFileSizeBytes;
	}

	public static PostMediaType from(String code) {
		return Arrays.stream(values())
				.filter(type -> type.name().equalsIgnoreCase(code))
				.findFirst()
				.orElseThrow(() -> new InvalidPostMediaTypeException(code));
	}
}
