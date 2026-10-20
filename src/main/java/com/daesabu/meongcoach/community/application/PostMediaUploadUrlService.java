package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.PostMediaUploadUrlIssuer;
import com.daesabu.meongcoach.community.application.provided.dto.PostMediaUploadUrlRequest;
import com.daesabu.meongcoach.community.domain.PostMediaType;
import com.daesabu.meongcoach.media.application.provided.CommunityMediaUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.CommunityMediaUploadUrlResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class PostMediaUploadUrlService implements PostMediaUploadUrlIssuer {

	private final CommunityMediaUploadUrlIssuer communityMediaUploadUrlIssuer;

	@Override
	public CommunityMediaUploadUrlResult issue(Long userId, PostMediaUploadUrlRequest request) {
		PostMediaType mediaType = PostMediaType.from(request.mediaType());
		if (mediaType == PostMediaType.VIDEO) {
			return communityMediaUploadUrlIssuer.issueVideo(userId, request.contentType(), request.fileSizeBytes());
		}
		return communityMediaUploadUrlIssuer.issueImage(userId, request.contentType());
	}
}
