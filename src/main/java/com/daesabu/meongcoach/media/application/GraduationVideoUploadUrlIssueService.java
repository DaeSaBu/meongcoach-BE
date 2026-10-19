package com.daesabu.meongcoach.media.application;

import com.daesabu.meongcoach.media.application.provided.GraduationVideoUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;
import com.daesabu.meongcoach.media.application.required.VideoStorage;
import com.daesabu.meongcoach.media.application.required.VideoUploadUrl;
import com.daesabu.meongcoach.media.domain.GraduationVideoFileSize;
import com.daesabu.meongcoach.media.domain.VideoObjectKey;
import com.daesabu.meongcoach.media.domain.VideoType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationVideoUploadUrlIssueService implements GraduationVideoUploadUrlIssuer {

	private final VideoStorage videoStorage;
	private final GraduationVideoUploadProperties properties;

	@Override
	public VideoUploadUrlResult issue(Long userId, Long topicId, String contentType, long fileSizeBytes) {
		VideoType videoType = VideoType.fromContentType(contentType);
		GraduationVideoFileSize fileSize = new GraduationVideoFileSize(fileSizeBytes, properties.maxFileSizeBytes());

		VideoObjectKey key = VideoObjectKey.createGraduation(userId, topicId, videoType);
		VideoUploadUrl uploadUrl = videoStorage.issueUploadUrl(key, videoType.getContentType(), fileSize.bytes());
		return new VideoUploadUrlResult(uploadUrl.uploadUrl(), uploadUrl.publicUrl(), uploadUrl.objectKey(),
				uploadUrl.expiresInSeconds());
	}
}
