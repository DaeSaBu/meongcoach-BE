package com.daesabu.meongcoach.media.adapter.webapi;

import com.daesabu.meongcoach.media.adapter.webapi.dto.ImageUploadUrlRequest;
import com.daesabu.meongcoach.media.adapter.webapi.dto.ImageUploadUrlResponse;
import com.daesabu.meongcoach.media.adapter.webapi.dto.VideoUploadCompletionRequest;
import com.daesabu.meongcoach.media.adapter.webapi.dto.VideoUploadCompletionResponse;
import com.daesabu.meongcoach.media.adapter.webapi.dto.VideoUploadUrlRequest;
import com.daesabu.meongcoach.media.adapter.webapi.dto.VideoUploadUrlResponse;
import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoUploadVerifier;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

	private final ImageUploadUrlIssuer imageUploadUrlIssuer;
	private final VideoUploadUrlIssuer videoUploadUrlIssuer;
	private final VideoUploadVerifier videoUploadVerifier;

	@PostMapping("/image-upload-urls")
	public ImageUploadUrlResponse issueImageUploadUrl(@CurrentUserId Long userId,
	                                                  @Valid @RequestBody ImageUploadUrlRequest request) {
		return ImageUploadUrlResponse.from(imageUploadUrlIssuer.issue(userId, request.target(), request.contentType()));
	}

	@PostMapping("/video-upload-urls")
	public VideoUploadUrlResponse issueVideoUploadUrl(@CurrentUserId Long userId,
	                                                  @Valid @RequestBody VideoUploadUrlRequest request) {
		return VideoUploadUrlResponse.from(videoUploadUrlIssuer.issue(userId, request.target(), request.contentType(),
				request.fileSizeBytes()));
	}

	@PostMapping("/video-upload-completions")
	public VideoUploadCompletionResponse verifyVideoUpload(@CurrentUserId Long userId,
	                                                       @Valid @RequestBody VideoUploadCompletionRequest request) {
		return VideoUploadCompletionResponse.from(videoUploadVerifier.verify(userId, request.objectKey()));
	}
}
