package com.daesabu.meongcoach.graduation.domain;

import com.daesabu.meongcoach.graduation.domain.exception.InvalidGraduationVideoKeyException;
import java.util.regex.Pattern;

public record GraduationVideoKey(String value) {

	private static final String KEY_PATTERN_FORMAT = "videos/graduation/%d/[^/]+\\.[A-Za-z0-9]+";

	public static GraduationVideoKey of(String objectKey, Long userId) {
		if (objectKey == null) {
			throw new InvalidGraduationVideoKeyException(null);
		}
		Pattern ownedKey = Pattern.compile(KEY_PATTERN_FORMAT.formatted(userId));
		if (!ownedKey.matcher(objectKey).matches()) {
			throw new InvalidGraduationVideoKeyException(objectKey);
		}
		return new GraduationVideoKey(objectKey);
	}
}
