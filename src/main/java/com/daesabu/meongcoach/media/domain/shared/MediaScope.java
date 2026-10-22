package com.daesabu.meongcoach.media.domain.shared;

import static org.springframework.util.Assert.isTrue;
import static org.springframework.util.Assert.notNull;

import java.util.regex.Pattern;

public record MediaScope(MediaKind kind, MediaVisibility visibility, String path, String subPath) {

	private static final Pattern SEGMENTS = Pattern.compile("[a-z0-9-]+(/[a-z0-9-]+)*");

	public MediaScope {
		notNull(kind, "미디어 종류가 필요하다");
		notNull(visibility, "공개 범위가 필요하다");
		isTrue(path != null && SEGMENTS.matcher(path).matches(), "경로 범위 형식이 올바르지 않다");
		isTrue(subPath == null || SEGMENTS.matcher(subPath).matches(), "하위 경로 형식이 올바르지 않다");
	}

	public static MediaScope of(MediaKind kind, MediaVisibility visibility, String path) {
		return new MediaScope(kind, visibility, path, null);
	}

	public MediaScope withSubPath(String subPath) {
		return new MediaScope(kind, visibility, path, subPath);
	}

	public String rootPrefix() {
		return kind.getDirectory() + "/" + path + "/";
	}

	public String prefixOf(Long userId) {
		notNull(userId, "사용자 ID가 필요하다");
		String userPrefix = rootPrefix() + userId + "/";
		if (subPath == null) {
			return userPrefix;
		}
		return userPrefix + subPath + "/";
	}
}
