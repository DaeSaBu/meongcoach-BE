package com.daesabu.meongcoach.ai.domain;

import com.daesabu.meongcoach.media.domain.shared.MediaKind;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import com.daesabu.meongcoach.media.domain.shared.MediaVisibility;

public final class AiVideo {

	public static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;
	public static final MediaScope SCOPE = MediaScope.of(MediaKind.VIDEO, MediaVisibility.PRIVATE, "training");

	private AiVideo() {
	}
}
