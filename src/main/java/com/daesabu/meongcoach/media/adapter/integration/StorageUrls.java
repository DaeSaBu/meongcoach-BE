package com.daesabu.meongcoach.media.adapter.integration;

import com.daesabu.meongcoach.media.domain.MediaObjectKey;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

final class StorageUrls {

	private StorageUrls() {
	}

	static String trimTrailingSlash(String url) {
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}

	static PutObjectRequest putObjectRequest(String bucket, MediaObjectKey key, String contentType,
			Long contentLength) {
		return PutObjectRequest.builder()
				.bucket(bucket)
				.key(key.value())
				.contentType(contentType)
				.contentLength(contentLength)
				.build();
	}
}
