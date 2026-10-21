package com.daesabu.meongcoach.dog.domain;

import com.daesabu.meongcoach.media.domain.shared.MediaKind;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import com.daesabu.meongcoach.media.domain.shared.MediaVisibility;

public final class DogProfileImage {

	public static final MediaScope SCOPE = MediaScope.of(MediaKind.IMAGE, MediaVisibility.PUBLIC, "dog-profile");

	private DogProfileImage() {
	}
}
