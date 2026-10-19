package com.daesabu.meongcoach.media.application;

import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;
import com.daesabu.meongcoach.media.application.required.VideoStorage;
import com.daesabu.meongcoach.media.application.required.VideoUploadUrl;
import com.daesabu.meongcoach.media.domain.VideoFileSize;
import com.daesabu.meongcoach.media.domain.VideoObjectKey;
import com.daesabu.meongcoach.media.domain.VideoType;
import com.daesabu.meongcoach.media.domain.VideoUploadTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
