package com.daesabu.meongcoach.media.application;

import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;
import com.daesabu.meongcoach.media.application.required.VideoStorage;
import com.daesabu.meongcoach.media.application.required.VideoUploadUrl;
import com.daesabu.meongcoach.media.domain.VideoType;
import com.daesabu.meongcoach.media.domain.VideoUploadTarget;
import com.daesabu.meongcoach.media.domain.vo.VideoFileSize;
import com.daesabu.meongcoach.media.domain.vo.VideoObjectKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 업로드 대상·영상 형식·파일 크기를 검증하고, 객체 키 생성을 도메인에 맡겨 스토리지에 업로드 URL 발급을 위임한다.
 */
@Service
@RequiredArgsConstructor
public class VideoUploadUrlIssueService implements VideoUploadUrlIssuer {

	private final VideoStorage videoStorage;

	@Override
	public VideoUploadUrlResult issue(Long userId, String target, String contentType, long fileSizeBytes) {
		VideoUploadTarget uploadTarget = VideoUploadTarget.from(target);
		VideoType videoType = VideoType.fromContentType(contentType);
		VideoFileSize fileSize = new VideoFileSize(fileSizeBytes);

		VideoObjectKey key = VideoObjectKey.create(uploadTarget, userId, videoType);
		VideoUploadUrl uploadUrl = videoStorage.issueUploadUrl(key, videoType.getContentType(), fileSize.bytes());
		return new VideoUploadUrlResult(uploadUrl.uploadUrl(), uploadUrl.publicUrl(), uploadUrl.objectKey(),
				uploadUrl.expiresInSeconds());
	}
}
