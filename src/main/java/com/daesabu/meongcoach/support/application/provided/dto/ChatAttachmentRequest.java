package com.daesabu.meongcoach.support.application.provided.dto;

import com.daesabu.meongcoach.support.domain.ChatAttachment;
import com.daesabu.meongcoach.support.domain.ChatAttachmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatAttachmentRequest(@NotBlank String type, @NotBlank @Size(max = 512) String objectKey) {
	public ChatAttachment toAttachment() {
		var mediaType = ChatAttachmentType.from(type);
		return new ChatAttachment(mediaType, objectKey);
	}
}
