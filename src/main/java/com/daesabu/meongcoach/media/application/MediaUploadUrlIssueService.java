package com.daesabu.meongcoach.media.application;

import com.daesabu.meongcoach.media.application.provided.MediaUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadRequest;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import com.daesabu.meongcoach.media.application.required.GcsStorage;
import com.daesabu.meongcoach.media.application.required.ObjectStorage;
import com.daesabu.meongcoach.media.application.required.R2Storage;
import com.daesabu.meongcoach.media.application.required.StorageUploadUrl;
import com.daesabu.meongcoach.media.domain.MediaFileSize;
import com.daesabu.meongcoach.media.domain.MediaObjectKey;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MediaUploadUrlIssueService implements MediaUploadUrlIssuer {

	private final R2Storage r2Storage;
	private final GcsStorage gcsStorage;

	@Override
	public MediaUploadUrlResult issueToR2(MediaUploadRequest request) {
		return issue(request, r2Storage);
	}

	@Override
	public MediaUploadUrlResult issueToGcs(MediaUploadRequest request) {
		return issue(request, gcsStorage);
	}

	private MediaUploadUrlResult issue(MediaUploadRequest request, ObjectStorage objectStorage) {
		MediaScope scope = request.scope();
		String extension = scope.kind().extensionOf(request.contentType());
		Long contentLength = contentLengthOf(request);

		MediaObjectKey key = MediaObjectKey.create(scope, request.userId(), extension);
		StorageUploadUrl uploadUrl = objectStorage.issueUploadUrl(key, request.contentType(), contentLength);
		return new MediaUploadUrlResult(uploadUrl.uploadUrl(), uploadUrl.publicUrl(), key.value(),
				uploadUrl.expiresInSeconds());
	}

	private Long contentLengthOf(MediaUploadRequest request) {
		if (request.maxFileSizeBytes() == null) {
			return null;
		}
		return new MediaFileSize(request.fileSizeBytes(), request.maxFileSizeBytes()).bytes();
	}
}
