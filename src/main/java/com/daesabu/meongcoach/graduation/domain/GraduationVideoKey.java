package com.daesabu.meongcoach.graduation.domain;

import com.daesabu.meongcoach.graduation.domain.exception.InvalidGraduationVideoKeyException;
import java.util.regex.Pattern;

/**
 * 실습 인증 영상의 객체 키. 영상은 토픽 단위로 올리므로 videos/graduation/{userId}/{topicId}/{파일} 형식이고,
 * 제출하는 회원 본인이 같은 토픽으로 받은 키만 받아들인다.
 */
public record GraduationVideoKey(String value) {

	private static final String KEY_PATTERN_FORMAT = "videos/graduation/%d/%d/[^/]+\\.[A-Za-z0-9]+";

	public static GraduationVideoKey of(String objectKey, Long userId, Long topicId) {
		if (objectKey == null) {
			throw new InvalidGraduationVideoKeyException(null);
		}
		Pattern ownedKey = Pattern.compile(KEY_PATTERN_FORMAT.formatted(userId, topicId));
		if (!ownedKey.matcher(objectKey).matches()) {
			throw new InvalidGraduationVideoKeyException(objectKey);
		}
		return new GraduationVideoKey(objectKey);
	}
}
