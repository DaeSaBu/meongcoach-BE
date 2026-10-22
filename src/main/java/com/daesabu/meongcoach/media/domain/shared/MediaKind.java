package com.daesabu.meongcoach.media.domain.shared;

import com.daesabu.meongcoach.media.domain.ImageType;
import com.daesabu.meongcoach.media.domain.VideoType;
import java.util.function.UnaryOperator;
import lombok.Getter;

public enum MediaKind {
	IMAGE("images", contentType -> ImageType.fromContentType(contentType).getExtension()),
	VIDEO("videos", contentType -> VideoType.fromContentType(contentType).getExtension()),
	;

	@Getter
	private final String directory;
	private final UnaryOperator<String> extensionResolver;

	MediaKind(String directory, UnaryOperator<String> extensionResolver) {
		this.directory = directory;
		this.extensionResolver = extensionResolver;
	}

	public String extensionOf(String contentType) {
		return extensionResolver.apply(contentType);
	}
}
