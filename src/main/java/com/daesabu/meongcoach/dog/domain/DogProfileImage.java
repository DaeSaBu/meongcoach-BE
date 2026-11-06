package com.daesabu.meongcoach.dog.domain;

import com.daesabu.meongcoach.media.domain.shared.MediaKind;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;

public final class DogProfileImage {

	public static final MediaScope SCOPE = MediaScope.of(MediaKind.IMAGE, "dog-profile");

	private DogProfileImage() {
	}
}
