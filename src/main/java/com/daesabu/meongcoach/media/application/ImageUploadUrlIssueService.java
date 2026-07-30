package com.daesabu.meongcoach.media.application;

import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlResult;
import com.daesabu.meongcoach.media.application.required.ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 업로드 대상·이미지 형식을 검증하고 객체 키를 만들어 스토리지에 업로드 URL 발급을 위임한다.
 */
@Service
@RequiredArgsConstructor
public class ImageUploadUrlIssueService implements ImageUploadUrlIssuer {

	private final ImageStorage imageStorage;

	@Override
	public ImageUploadUrlResult issue(Long userId, String target, String contentType) {
		throw new UnsupportedOperationException("아직 구현되지 않았다");
	}
}
