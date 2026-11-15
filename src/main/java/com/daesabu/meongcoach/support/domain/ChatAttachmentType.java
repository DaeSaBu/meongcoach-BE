package com.daesabu.meongcoach.support.domain;

import com.daesabu.meongcoach.media.domain.shared.MediaKind;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import com.daesabu.meongcoach.support.domain.exception.InvalidChatAttachmentException;
import java.util.Arrays;
import lombok.Getter;

@Getter
public enum ChatAttachmentType {
	IMAGE(MediaKind.IMAGE, 20L * 1024 * 1024),
	VIDEO(MediaKind.VIDEO, 512L * 1024 * 1024);

	private final MediaScope scope;
	private final long maxFileSizeBytes;

	ChatAttachmentType(MediaKind kind, long maxFileSizeBytes) {
		this.scope = MediaScope.of(kind, "support-chat");
		this.maxFileSizeBytes = maxFileSizeBytes;
	}

	public static ChatAttachmentType from(String value) {
		return Arrays.stream(values()).filter(type -> type.name().equalsIgnoreCase(value))
				.findFirst().orElseThrow(InvalidChatAttachmentException::new);
	}
}
