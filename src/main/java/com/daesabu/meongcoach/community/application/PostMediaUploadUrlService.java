package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.PostMediaUploadUrlIssuer;
import com.daesabu.meongcoach.community.application.provided.dto.PostMediaUploadUrlRequest;
import com.daesabu.meongcoach.community.domain.PostMediaType;
import com.daesabu.meongcoach.media.application.provided.MediaUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadRequest;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class PostMediaUploadUrlService implements PostMediaUploadUrlIssuer {

	private final MediaUploadUrlIssuer mediaUploadUrlIssuer;

	@Override
	public MediaUploadUrlResult issue(Long userId, PostMediaUploadUrlRequest request) {
		PostMediaType mediaType = PostMediaType.from(request.mediaType());
		MediaUploadRequest uploadRequest = new MediaUploadRequest(mediaType.getMediaScope(), userId,
				request.contentType(), request.fileSizeBytes(), mediaType.getMaxFileSizeBytes());
		return mediaUploadUrlIssuer.issue(uploadRequest);
	}
}
