package com.daesabu.meongcoach.media.application;

import com.daesabu.meongcoach.media.application.provided.MediaUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadRequest;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import com.daesabu.meongcoach.media.application.required.PrivateObjectStorage;
import com.daesabu.meongcoach.media.application.required.PublicObjectStorage;
import com.daesabu.meongcoach.media.application.required.StorageUploadUrl;
import com.daesabu.meongcoach.media.domain.MediaFileSize;
import com.daesabu.meongcoach.media.domain.MediaObjectKey;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import com.daesabu.meongcoach.media.domain.shared.MediaVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MediaUploadUrlIssueService implements MediaUploadUrlIssuer {

	private final PublicObjectStorage publicObjectStorage;
	private final PrivateObjectStorage privateObjectStorage;

	@Override
	public MediaUploadUrlResult issue(MediaUploadRequest request) {
		MediaScope scope = request.scope();
		String extension = scope.kind().extensionOf(request.contentType());
		Long contentLength = contentLengthOf(request);

		MediaObjectKey key = MediaObjectKey.create(scope, request.userId(), extension);
		StorageUploadUrl uploadUrl = issueUploadUrl(scope.visibility(), key, request.contentType(), contentLength);
		return new MediaUploadUrlResult(uploadUrl.uploadUrl(), uploadUrl.publicUrl(), key.value(),
				uploadUrl.expiresInSeconds());
	}

	private Long contentLengthOf(MediaUploadRequest request) {
		if (request.maxFileSizeBytes() == null) {
			return null;
		}
		return new MediaFileSize(request.fileSizeBytes(), request.maxFileSizeBytes()).bytes();
	}

	private StorageUploadUrl issueUploadUrl(MediaVisibility visibility, MediaObjectKey key, String contentType,
			Long contentLength) {
		if (visibility == MediaVisibility.PRIVATE) {
			return privateObjectStorage.issueUploadUrl(key, contentType, contentLength);
		}
		return publicObjectStorage.issueUploadUrl(key, contentType, contentLength);
	}
}
