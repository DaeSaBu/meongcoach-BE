package com.daesabu.meongcoach.user.domain;

import com.daesabu.meongcoach.media.domain.shared.MediaKind;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import com.daesabu.meongcoach.media.domain.shared.MediaVisibility;

public final class UserProfileImage {

	public static final MediaScope SCOPE = MediaScope.of(MediaKind.IMAGE, MediaVisibility.PUBLIC, "user-profile");

	private UserProfileImage() {
	}
}
