package com.daesabu.meongcoach.media.adapter.webapi;

import com.daesabu.meongcoach.media.adapter.webapi.dto.ImageUploadUrlRequest;
import com.daesabu.meongcoach.media.adapter.webapi.dto.ImageUploadUrlResponse;
import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlIssuer;
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

	@PostMapping("/presigned-urls")
	public ImageUploadUrlResponse issueImageUploadUrl(@CurrentUserId Long userId,
	                                                  @Valid @RequestBody ImageUploadUrlRequest request) {
		return ImageUploadUrlResponse.from(imageUploadUrlIssuer.issue(userId, request.target(), request.contentType()));
	}
}
