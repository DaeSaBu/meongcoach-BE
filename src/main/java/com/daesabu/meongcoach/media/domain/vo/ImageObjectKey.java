package com.daesabu.meongcoach.media.domain.vo;

import com.daesabu.meongcoach.media.domain.ImageType;
import com.daesabu.meongcoach.media.domain.ImageUploadTarget;
import java.util.UUID;

/**
 * 스토리지 객체 키 값 객체. images/{대상}/{사용자}/{UUID}.{확장자} 규칙을 이 타입이 소유한다.
 * 인스턴스는 create로만 만든다.
 */
public record ImageObjectKey(String value) {

	private static final String FORMAT = "images/%s/%d/%s.%s";

	/**
	 * 업로드 대상·사용자·이미지 형식으로 객체 키를 만든다.
	 */
	// UUID 키라 대상당 이미지가 쌓이지만, 삭제·정리는 스토리지 수명 주기 정책에 맡긴다
	public static ImageObjectKey create(ImageUploadTarget target, Long userId, ImageType imageType) {
		return new ImageObjectKey(
				FORMAT.formatted(target.getPathSegment(), userId, UUID.randomUUID(), imageType.getExtension()));
	}
}
