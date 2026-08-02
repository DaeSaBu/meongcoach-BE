package com.daesabu.meongcoach.media.domain.vo;

import com.daesabu.meongcoach.media.domain.VideoType;
import com.daesabu.meongcoach.media.domain.VideoUploadTarget;
import com.daesabu.meongcoach.media.domain.exception.InvalidVideoObjectKeyException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 영상 스토리지 객체 키 값 객체. videos/{대상}/{사용자}/{UUID}.{확장자} 규칙을 이 타입이 소유한다.
 * 인스턴스는 create 또는 parse로만 만든다.
 */
public record VideoObjectKey(String value) {

	private static final String FORMAT = "videos/%s/%d/%s.%s";
	// 대상 구획은 이후 추가될 수 있어 enum과 결합하지 않고 형식만 검증한다
	private static final Pattern KEY_PATTERN = Pattern.compile("videos/[a-z0-9-]+/([0-9]+)/[^/]+\\.[A-Za-z0-9]+");

	/**
	 * 업로드 대상·사용자·영상 형식으로 객체 키를 만든다.
	 */
	// 사용자 ID를 경로에 넣어 두면 이후 재생 URL 발급에서 소유자 검증에 그대로 쓸 수 있다
	public static VideoObjectKey create(VideoUploadTarget target, Long userId, VideoType videoType) {
		return new VideoObjectKey(
				FORMAT.formatted(target.getPathSegment(), userId, UUID.randomUUID(), videoType.getExtension()));
	}

	/**
	 * 저장된 객체 키 문자열을 검증해 값 객체로 만든다. 규칙에 어긋나는 키는 거부한다.
	 */
	public static VideoObjectKey parse(String value) {
		if (value == null || !KEY_PATTERN.matcher(value).matches()) {
			throw new InvalidVideoObjectKeyException(value);
		}
		return new VideoObjectKey(value);
	}

	/**
	 * 키 경로에 담긴 소유자(업로더)의 사용자 ID를 돌려준다.
	 */
	public Long ownerUserId() {
		return Long.parseLong(value.split("/")[2]);
	}
}
