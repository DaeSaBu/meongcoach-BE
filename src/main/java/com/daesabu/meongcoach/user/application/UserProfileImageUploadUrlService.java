package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.media.application.provided.MediaUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadRequest;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import com.daesabu.meongcoach.user.application.provided.UserProfileImageUploadUrlIssuer;
import com.daesabu.meongcoach.user.domain.UserProfileImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileImageUploadUrlService implements UserProfileImageUploadUrlIssuer {

	private final MediaUploadUrlIssuer mediaUploadUrlIssuer;

	@Override
	public MediaUploadUrlResult issue(Long userId, String contentType) {
		MediaUploadRequest uploadRequest = MediaUploadRequest.image(UserProfileImage.SCOPE, userId, contentType);
		return mediaUploadUrlIssuer.issue(uploadRequest);
	}
}
