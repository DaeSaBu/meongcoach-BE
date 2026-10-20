package com.daesabu.meongcoach.media.domain;

import java.util.UUID;

public record ImageObjectKey(String value) {

	private static final String FORMAT = "images/%s/%d/%s.%s";
	private static final String COMMUNITY_PATH_SEGMENT = "community-post";

	public static ImageObjectKey create(ImageUploadTarget target, Long userId, ImageType imageType) {
		return new ImageObjectKey(
				FORMAT.formatted(target.getPathSegment(), userId, UUID.randomUUID(), imageType.getExtension()));
	}

	public static ImageObjectKey createCommunity(Long userId, ImageType imageType) {
		return new ImageObjectKey(
				FORMAT.formatted(COMMUNITY_PATH_SEGMENT, userId, UUID.randomUUID(), imageType.getExtension()));
	}
}
