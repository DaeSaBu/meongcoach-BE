package com.daesabu.meongcoach.media.application;

import com.daesabu.meongcoach.media.application.provided.StoredImageUrlValidator;
import com.daesabu.meongcoach.media.application.required.ImageStorage;
import com.daesabu.meongcoach.media.domain.exception.InvalidImageUrlException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 클라이언트가 보낸 이미지 URL이 우리 스토리지의 공개 URL인지 검증한다.
 */
@Service
@RequiredArgsConstructor
public class StoredImageUrlValidateService implements StoredImageUrlValidator {

	private final ImageStorage imageStorage;

	@Override
	public void validate(String url) {
		// null·빈 문자열은 이미지 미설정이므로 검증하지 않는다
		if (url == null || url.isBlank()) {
			return;
		}
		if (!imageStorage.isPublicUrl(url)) {
			throw new InvalidImageUrlException();
		}
	}
}
