package com.daesabu.meongcoach.dog.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileImageUploadUrlIssuer;
import com.daesabu.meongcoach.dog.domain.shared.DogProfileImage;
import com.daesabu.meongcoach.media.application.provided.MediaUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadRequest;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DogProfileImageUploadUrlService implements DogProfileImageUploadUrlIssuer {

	private final MediaUploadUrlIssuer mediaUploadUrlIssuer;

	@Override
	public MediaUploadUrlResult issue(Long userId, String contentType) {
		MediaUploadRequest uploadRequest = MediaUploadRequest.image(DogProfileImage.SCOPE, userId, contentType);
		return mediaUploadUrlIssuer.issue(uploadRequest);
	}
}
