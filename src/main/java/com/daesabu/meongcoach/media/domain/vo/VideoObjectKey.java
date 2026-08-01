package com.daesabu.meongcoach.media.domain.vo;

import com.daesabu.meongcoach.media.domain.VideoType;
import com.daesabu.meongcoach.media.domain.VideoUploadTarget;
import java.util.UUID;

/**
 * 영상 스토리지 객체 키 값 객체. videos/{대상}/{사용자}/{UUID}.{확장자} 규칙을 이 타입이 소유한다.
 * 인스턴스는 create로만 만든다.
 */
public record VideoObjectKey(String value) {

	private static final String FORMAT = "videos/%s/%d/%s.%s";

	/**
	 * 업로드 대상·사용자·영상 형식으로 객체 키를 만든다.
	 */
	// 사용자 ID를 경로에 넣어 두면 이후 재생 URL 발급에서 소유자 검증에 그대로 쓸 수 있다
	public static VideoObjectKey create(VideoUploadTarget target, Long userId, VideoType videoType) {
		return new VideoObjectKey(
				FORMAT.formatted(target.getPathSegment(), userId, UUID.randomUUID(), videoType.getExtension()));
	}
}
