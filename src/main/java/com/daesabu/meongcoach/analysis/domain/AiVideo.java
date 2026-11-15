package com.daesabu.meongcoach.analysis.domain;

import com.daesabu.meongcoach.analysis.domain.exception.AiVideoDurationOutOfRangeException;
import com.daesabu.meongcoach.media.domain.shared.MediaKind;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;

public final class AiVideo {

	public static final long MAX_FILE_SIZE_BYTES = 512L * 1024 * 1024;
	public static final long MIN_DURATION_SECONDS = 10L;
	public static final long MAX_DURATION_SECONDS = 300L;
	public static final MediaScope SCOPE = MediaScope.of(MediaKind.VIDEO, "training");

	private AiVideo() {
	}

	public static void validateDuration(long durationSeconds) {
		if (durationSeconds < MIN_DURATION_SECONDS || durationSeconds > MAX_DURATION_SECONDS) {
			throw new AiVideoDurationOutOfRangeException();
		}
	}
}
