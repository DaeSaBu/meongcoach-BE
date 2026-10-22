package com.daesabu.meongcoach.media.domain;

import com.daesabu.meongcoach.media.domain.exception.InvalidMediaObjectKeyException;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import java.util.UUID;
import java.util.regex.Pattern;

public record MediaObjectKey(String value) {

	private static final String FILE_NAME_PATTERN = "[^/]+\\.[A-Za-z0-9]+";
	private static final String ANY_SUB_PATH_PATTERN = "(?:[a-z0-9-]+/)*";
	private static final Pattern FILE_NAME = Pattern.compile(FILE_NAME_PATTERN);

	public static MediaObjectKey create(MediaScope scope, Long userId, String extension) {
		return new MediaObjectKey(scope.prefixOf(userId) + UUID.randomUUID() + "." + extension);
	}

	public static MediaObjectKey parse(MediaScope scope, String value) {
		if (value == null) {
			throw new InvalidMediaObjectKeyException(null);
		}
		Pattern underScope = Pattern.compile(
				Pattern.quote(scope.rootPrefix()) + "[0-9]+/" + subPathPattern(scope) + FILE_NAME_PATTERN);
		if (!underScope.matcher(value).matches()) {
			throw new InvalidMediaObjectKeyException(value);
		}
		return new MediaObjectKey(value);
	}

	private static String subPathPattern(MediaScope scope) {
		if (scope.subPath() == null) {
			return ANY_SUB_PATH_PATTERN;
		}
		return Pattern.quote(scope.subPath() + "/");
	}

	public boolean belongsTo(MediaScope scope, Long userId) {
		String prefix = scope.prefixOf(userId);
		return value.startsWith(prefix) && FILE_NAME.matcher(value.substring(prefix.length())).matches();
	}
}
