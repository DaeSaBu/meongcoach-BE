package com.daesabu.meongcoach.media.application;

import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlResult;
import com.daesabu.meongcoach.media.application.required.ImageStorage;
import com.daesabu.meongcoach.media.application.required.ImageUploadUrl;
import com.daesabu.meongcoach.media.domain.ImageType;
import com.daesabu.meongcoach.media.domain.ImageUploadTarget;
import com.daesabu.meongcoach.media.domain.vo.ImageObjectKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 업로드 대상·이미지 형식을 검증하고, 객체 키 생성을 도메인에 맡겨 스토리지에 업로드 URL 발급을 위임한다.
 */
@Service
@RequiredArgsConstructor
public class ImageUploadUrlIssueService implements ImageUploadUrlIssuer {

	private final ImageStorage imageStorage;

	@Override
	public ImageUploadUrlResult issue(Long userId, String target, String contentType) {
		ImageUploadTarget uploadTarget = ImageUploadTarget.from(target);
		ImageType imageType = ImageType.fromContentType(contentType);

		ImageObjectKey key = ImageObjectKey.create(uploadTarget, userId, imageType);
		ImageUploadUrl uploadUrl = imageStorage.issueUploadUrl(key, imageType.getContentType());
		return new ImageUploadUrlResult(uploadUrl.uploadUrl(), uploadUrl.publicUrl(), uploadUrl.expiresInSeconds());
	}
}
