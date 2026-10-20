package com.daesabu.meongcoach.media.application.provided;

import com.daesabu.meongcoach.media.domain.shared.MediaScope;

/**
 * 비공개 스토리지에 올린 미디어를 읽을 수 있는 다운로드 URL 발급 능력.
 */
public interface MediaDownloadUrlIssuer {

	/**
	 * 범위 아래의 객체 키면 유효 시간이 있는 다운로드 URL을 반환한다.
	 * 범위 아래의 객체 키가 아니면 InvalidMediaObjectKeyException을 던진다.
	 */
	String issue(MediaScope scope, String objectKey);
}
