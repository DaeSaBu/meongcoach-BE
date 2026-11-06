package com.daesabu.meongcoach.graduation.domain;

import com.daesabu.meongcoach.media.domain.shared.MediaKind;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;

public final class GraduationVideo {

	public static final long MAX_FILE_SIZE_BYTES = 512L * 1024 * 1024;
	public static final MediaScope SCOPE = MediaScope.of(MediaKind.VIDEO, "graduation");

	private GraduationVideo() {
	}

	public static MediaScope scopeOf(Long topicId) {
		return SCOPE.withSubPath(String.valueOf(topicId));
	}
}
