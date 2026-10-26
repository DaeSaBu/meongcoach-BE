package com.daesabu.meongcoach.media.application.provided.dto;

import com.daesabu.meongcoach.media.domain.shared.MediaScope;

public record MediaUploadRequest(
		MediaScope scope,
		Long userId,
		String contentType,
		Long fileSizeBytes,
		Long maxFileSizeBytes) {

	public static MediaUploadRequest image(MediaScope scope, Long userId, String contentType) {
		return new MediaUploadRequest(scope, userId, contentType, null, null);
	}

	public static MediaUploadRequest video(MediaScope scope, Long userId, String contentType, Long fileSizeBytes,
			long maxFileSizeBytes) {
		return new MediaUploadRequest(scope, userId, contentType, fileSizeBytes, maxFileSizeBytes);
	}
}
