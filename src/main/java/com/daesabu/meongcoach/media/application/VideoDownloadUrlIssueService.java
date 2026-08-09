package com.daesabu.meongcoach.media.application;

import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlResult;
import com.daesabu.meongcoach.media.application.required.VideoDownloadUrl;
import com.daesabu.meongcoach.media.application.required.VideoStorage;
import com.daesabu.meongcoach.media.domain.vo.VideoObjectKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 객체 키를 검증하고 스토리지에 다운로드 URL 발급을 위임한다. 소유자 ID는 키 경로에서 추출해 함께 돌려준다.
 */
@Service
@RequiredArgsConstructor
public class VideoDownloadUrlIssueService implements VideoDownloadUrlIssuer {

	private final VideoStorage videoStorage;

	@Override
	public VideoDownloadUrlResult issue(String objectKey) {
		VideoObjectKey key = VideoObjectKey.parse(objectKey);

		VideoDownloadUrl downloadUrl = videoStorage.issueDownloadUrl(key);
		return new VideoDownloadUrlResult(downloadUrl.downloadUrl(), downloadUrl.publicUrl(),
				key.ownerUserId(), downloadUrl.expiresInSeconds());
	}
}
