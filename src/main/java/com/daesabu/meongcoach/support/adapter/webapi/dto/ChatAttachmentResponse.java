package com.daesabu.meongcoach.support.adapter.webapi.dto;

import com.daesabu.meongcoach.support.domain.ChatAttachment;
import com.daesabu.meongcoach.support.domain.ChatAttachmentType;

public record ChatAttachmentResponse(ChatAttachmentType type) {
	public static ChatAttachmentResponse from(ChatAttachment attachment) {
		if (attachment == null) {
			return null;
		}
		return new ChatAttachmentResponse(attachment.getType());
	}
}
