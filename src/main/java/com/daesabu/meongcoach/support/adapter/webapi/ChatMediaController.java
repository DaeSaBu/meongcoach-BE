package com.daesabu.meongcoach.support.adapter.webapi;

import com.daesabu.meongcoach.shared.security.CurrentUserId;
import com.daesabu.meongcoach.support.adapter.webapi.dto.ChatMediaDownloadResponse;
import com.daesabu.meongcoach.support.adapter.webapi.dto.ChatMediaUploadResponse;
import com.daesabu.meongcoach.support.application.provided.ChatMediaUrlIssuer;
import com.daesabu.meongcoach.support.application.provided.dto.ChatMediaUploadRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support/conversations/{id}")
@RequiredArgsConstructor
public class ChatMediaController {
	private final ChatMediaUrlIssuer media;

	@PostMapping("/attachments/upload-url")
	public ChatMediaUploadResponse upload(@CurrentUserId Long userId, @PathVariable @Positive long id,
			@Valid @RequestBody ChatMediaUploadRequest request) {
		var result = media.upload(userId, id, request);
		return ChatMediaUploadResponse.from(result);
	}

	@GetMapping("/messages/{messageId}/attachment")
	public ChatMediaDownloadResponse download(@CurrentUserId Long userId, @PathVariable @Positive long id,
			@PathVariable @Positive long messageId) {
		String url = media.download(userId, id, messageId);
		return new ChatMediaDownloadResponse(url);
	}
}
