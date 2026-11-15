package com.daesabu.meongcoach.support.domain;

import com.daesabu.meongcoach.support.domain.exception.InvalidChatAttachmentException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatAttachment {
	@Enumerated(EnumType.STRING)
	@Column(name = "media_type", length = 20)
	private ChatAttachmentType type;

	@Column(name = "media_object_key", length = 512)
	private String objectKey;

	public ChatAttachment(ChatAttachmentType type, String objectKey) {
		if (type == null || objectKey == null || objectKey.isBlank() || objectKey.length() > 512) {
			throw new InvalidChatAttachmentException();
		}
		this.type = type;
		this.objectKey = objectKey;
	}
}
