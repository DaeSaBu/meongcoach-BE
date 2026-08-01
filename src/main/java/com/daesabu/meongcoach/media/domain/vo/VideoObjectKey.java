package com.daesabu.meongcoach.media.domain.vo;

import com.daesabu.meongcoach.media.domain.VideoType;
import com.daesabu.meongcoach.media.domain.VideoUploadTarget;
import com.daesabu.meongcoach.media.domain.exception.InvalidObjectKeyException;
import com.daesabu.meongcoach.media.domain.exception.InvalidUploadTargetException;
import com.daesabu.meongcoach.media.domain.exception.UnsupportedVideoTypeException;
import java.util.UUID;

/**
 * 영상 스토리지 객체 키 값 객체. videos/{대상경로}/{사용자ID}/{UUID}.{확장자} 규칙과 "이 키가 누구 것인가" 판별을 이 타입이 소유한다.
 * 인스턴스는 새로 만들 때 create, 클라이언트가 돌려준 문자열을 되돌릴 때 parse로 만든다.
 */
public record VideoObjectKey(String value) {

	private static final String FORMAT = "videos/%s/%d/%s.%s";
	private static final String PREFIX = "videos";
	private static final String SEPARATOR = "/";
	private static final String PARENT_PATH = "..";
	private static final char EXTENSION_SEPARATOR = '.';
	private static final int SEGMENT_COUNT = 4;
	private static final int TARGET_INDEX = 1;
	private static final int OWNER_INDEX = 2;
	private static final int FILE_NAME_INDEX = 3;

	// 검증을 정규 생성자에 두어 parse를 거치지 않고 만든 인스턴스도 형식을 벗어날 수 없게 한다.
	// create가 만드는 문자열도 같은 규칙을 그대로 통과하므로 생성 경로와 파싱 경로가 한 기준을 쓴다
	public VideoObjectKey {
		validate(value);
	}

	/**
	 * 업로드 대상·사용자·영상 형식으로 객체 키를 만든다. 파일명을 서버가 UUID로 정해 클라이언트가 경로를 지정하지 못하게 한다.
	 */
	public static VideoObjectKey create(VideoUploadTarget target, Long userId, VideoType videoType) {
		return new VideoObjectKey(
				FORMAT.formatted(target.getPathSegment(), userId, UUID.randomUUID(), videoType.getExtension()));
	}

	/**
	 * 문자열을 검증하며 객체 키로 되돌린다. 우리가 만든 형식이 아니면 예외를 던진다.
	 */
	public static VideoObjectKey parse(String value) {
		return new VideoObjectKey(value);
	}

	/**
	 * 키에 담긴 소유자의 사용자 ID를 꺼낸다.
	 */
	public Long ownerId() {
		return Long.valueOf(value.split(SEPARATOR)[OWNER_INDEX]);
	}

	/**
	 * 키의 소유자가 주어진 사용자인지 확인한다.
	 */
	public boolean belongsTo(Long userId) {
		return ownerId().equals(userId);
	}

	private static void validate(String value) {
		if (value == null || value.isBlank()) {
			throw new InvalidObjectKeyException(value);
		}
		// 경로 탈출을 막는다. 조각 수 검사보다 먼저 두어 상위 경로 표기가 조각으로 세어지기 전에 걸러낸다
		if (value.contains(PARENT_PATH) || value.startsWith(SEPARATOR)) {
			throw new InvalidObjectKeyException(value);
		}
		String[] segments = value.split(SEPARATOR);
		if (segments.length != SEGMENT_COUNT || !PREFIX.equals(segments[0])) {
			throw new InvalidObjectKeyException(value);
		}
		validateTarget(value, segments[TARGET_INDEX]);
		validateOwnerId(value, segments[OWNER_INDEX]);
		validateExtension(value, segments[FILE_NAME_INDEX]);
	}

	// 대상·형식 검증은 각자의 도메인 예외를 던지므로, 키 형식 오류라는 사실이 남도록 우리 예외로 바꿔 던진다
	private static void validateTarget(String value, String pathSegment) {
		try {
			VideoUploadTarget.fromPathSegment(pathSegment);
		} catch (InvalidUploadTargetException e) {
			throw new InvalidObjectKeyException(value);
		}
	}

	private static void validateOwnerId(String value, String ownerId) {
		try {
			if (Long.parseLong(ownerId) <= 0) {
				throw new InvalidObjectKeyException(value);
			}
		} catch (NumberFormatException e) {
			throw new InvalidObjectKeyException(value);
		}
	}

	private static void validateExtension(String value, String fileName) {
		int extensionIndex = fileName.lastIndexOf(EXTENSION_SEPARATOR);
		if (extensionIndex < 0) {
			throw new InvalidObjectKeyException(value);
		}
		try {
			VideoType.fromExtension(fileName.substring(extensionIndex + 1));
		} catch (UnsupportedVideoTypeException e) {
			throw new InvalidObjectKeyException(value);
		}
	}
}
