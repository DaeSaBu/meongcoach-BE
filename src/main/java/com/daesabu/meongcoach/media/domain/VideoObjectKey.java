package com.daesabu.meongcoach.media.domain;

import static org.springframework.util.Assert.isTrue;

import com.daesabu.meongcoach.media.domain.exception.InvalidVideoObjectKeyException;
import java.util.UUID;
import java.util.regex.Pattern;

public record VideoObjectKey(String value) {

	private static final String FORMAT = "videos/%s/%d/%s.%s";
	private static final String GRADUATION_FORMAT = "videos/graduation/%d/%d/%s.%s";
	private static final Pattern KEY_PATTERN =
			Pattern.compile("videos/[a-z0-9-]+/([0-9]+)/(?:[0-9]+/)?[^/]+\\.[A-Za-z0-9]+");

	public static VideoObjectKey create(VideoUploadTarget target, Long userId, VideoType videoType) {
		String value = FORMAT.formatted(target.getPathSegment(), userId, UUID.randomUUID(), videoType.getExtension());
		return new VideoObjectKey(value);
	}

	public static VideoObjectKey createGraduation(Long userId, Long topicId, VideoType videoType) {
		isTrue(topicId != null, "실습 인증 영상에는 토픽 ID가 필요하다");
		String value = GRADUATION_FORMAT.formatted(userId, topicId, UUID.randomUUID(), videoType.getExtension());
		return new VideoObjectKey(value);
	}

	public static VideoObjectKey parse(String value) {
		if (value == null || !KEY_PATTERN.matcher(value).matches()) {
			throw new InvalidVideoObjectKeyException(value);
		}
		return new VideoObjectKey(value);
	}

	public Long ownerUserId() {
		return Long.parseLong(value.split("/")[2]);
	}
}
