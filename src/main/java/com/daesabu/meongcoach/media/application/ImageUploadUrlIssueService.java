package com.daesabu.meongcoach.media.application;

import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlResult;
import com.daesabu.meongcoach.media.application.required.ImageStorage;
import com.daesabu.meongcoach.media.application.required.ImageUploadUrl;
import com.daesabu.meongcoach.media.domain.ImageType;
import com.daesabu.meongcoach.media.domain.ImageUploadTarget;
import java.util.UUID;
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
		ImageUploadTarget uploadTarget = ImageUploadTarget.from(target);
		ImageType imageType = ImageType.fromContentType(contentType);

		// UUID 키라 대상당 이미지가 쌓이지만, 삭제·정리는 스토리지 수명 주기 정책에 맡긴다
		String key = "images/%s/%d/%s.%s"
				.formatted(uploadTarget.getPathSegment(), userId, UUID.randomUUID(), imageType.getExtension());
		ImageUploadUrl uploadUrl = imageStorage.issueUploadUrl(key, imageType.getContentType());
		return new ImageUploadUrlResult(uploadUrl.uploadUrl(), uploadUrl.publicUrl(), uploadUrl.expiresInSeconds());
	}
}
