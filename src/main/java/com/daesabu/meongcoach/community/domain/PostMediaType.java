package com.daesabu.meongcoach.community.domain;

import lombok.Getter;

@Getter
public enum PostMediaType {
	IMAGE("사진", 3),
	VIDEO("영상", 2);

	private final String displayName;
	private final int maxCount;

	PostMediaType(String displayName, int maxCount) {
		this.displayName = displayName;
		this.maxCount = maxCount;
	}
}
