package com.daesabu.meongcoach.support.application.provided;

import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import com.daesabu.meongcoach.support.application.provided.dto.ChatMediaUploadRequest;
import jakarta.validation.Valid;

public interface ChatMediaUrlIssuer {
	/**
	 * 접근 가능한 진행 중 대화에 첨부할 본인 파일의 업로드 URL을 발급한다.
	 */
	MediaUploadUrlResult upload(Long userId, long conversationId, @Valid ChatMediaUploadRequest request);

	/**
	 * 접근 가능한 진행 중 대화의 메시지 첨부에 한정된 다운로드 URL을 발급한다.
	 */
	String download(Long userId, long conversationId, long messageId);
}
