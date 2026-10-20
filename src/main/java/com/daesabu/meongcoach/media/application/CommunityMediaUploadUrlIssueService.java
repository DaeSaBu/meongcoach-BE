package com.daesabu.meongcoach.media.application;

import com.daesabu.meongcoach.media.application.provided.CommunityMediaUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.CommunityMediaUploadUrlResult;
import com.daesabu.meongcoach.media.application.required.ImageStorage;
import com.daesabu.meongcoach.media.application.required.ImageUploadUrl;
import com.daesabu.meongcoach.media.application.required.PublicVideoStorage;
import com.daesabu.meongcoach.media.application.required.VideoUploadUrl;
import com.daesabu.meongcoach.media.domain.CommunityVideoFileSize;
import com.daesabu.meongcoach.media.domain.ImageObjectKey;
import com.daesabu.meongcoach.media.domain.ImageType;
import com.daesabu.meongcoach.media.domain.VideoObjectKey;
import com.daesabu.meongcoach.media.domain.VideoType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunityMediaUploadUrlIssueService implements CommunityMediaUploadUrlIssuer {

	private final ImageStorage imageStorage;
	private final PublicVideoStorage publicVideoStorage;

	@Override
	public CommunityMediaUploadUrlResult issueImage(Long userId, String contentType) {
		ImageType imageType = ImageType.fromContentType(contentType);

		ImageObjectKey key = ImageObjectKey.createCommunity(userId, imageType);
		ImageUploadUrl uploadUrl = imageStorage.issueUploadUrl(key, imageType.getContentType());
		return new CommunityMediaUploadUrlResult(uploadUrl.uploadUrl(), uploadUrl.publicUrl(),
				uploadUrl.expiresInSeconds());
	}

	@Override
	public CommunityMediaUploadUrlResult issueVideo(Long userId, String contentType, long fileSizeBytes) {
		VideoType videoType = VideoType.fromContentType(contentType);
		CommunityVideoFileSize fileSize = new CommunityVideoFileSize(fileSizeBytes);

		VideoObjectKey key = VideoObjectKey.createCommunity(userId, videoType);
		VideoUploadUrl uploadUrl = publicVideoStorage.issueUploadUrl(key, videoType.getContentType(),
				fileSize.bytes());
		return new CommunityMediaUploadUrlResult(uploadUrl.uploadUrl(), uploadUrl.publicUrl(),
				uploadUrl.expiresInSeconds());
	}
}
