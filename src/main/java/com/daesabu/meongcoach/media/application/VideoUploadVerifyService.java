package com.daesabu.meongcoach.media.application;

import com.daesabu.meongcoach.media.application.provided.VerifiedVideoResult;
import com.daesabu.meongcoach.media.application.provided.VideoUploadVerifier;
import com.daesabu.meongcoach.media.application.required.StoredVideo;
import com.daesabu.meongcoach.media.application.required.VideoStorage;
import com.daesabu.meongcoach.media.domain.VideoType;
import com.daesabu.meongcoach.media.domain.exception.VideoAccessDeniedException;
import com.daesabu.meongcoach.media.domain.exception.VideoNotUploadedException;
import com.daesabu.meongcoach.media.domain.vo.VideoFileSize;
import com.daesabu.meongcoach.media.domain.vo.VideoObjectKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 클라이언트가 돌려준 객체 키의 형식과 소유권을 확인한 뒤, 스토리지에 직접 물어 영상이 실제로 저장됐는지 검증한다.
 * 형식과 크기는 발급 시점의 신고값이 아니라 스토리지가 보고한 실제 값으로 다시 검증한다.
 */
@Service
@RequiredArgsConstructor
public class VideoUploadVerifyService implements VideoUploadVerifier {

	private final VideoStorage videoStorage;

	@Override
	public VerifiedVideoResult verify(Long userId, String objectKey) {
		VideoObjectKey key = VideoObjectKey.parse(objectKey);
		// 소유권 검사를 스토리지 조회보다 먼저 해서 남의 키가 존재하는지를 응답 차이로 유추당하지 않게 한다
		if (!key.belongsTo(userId)) {
			throw new VideoAccessDeniedException(key.value());
		}

		StoredVideo storedVideo = videoStorage.findStoredVideo(key)
				.orElseThrow(() -> new VideoNotUploadedException(key.value()));
		// 두 호출은 값을 쓰기 위해서가 아니라 실제 저장된 형식·크기가 우리 정책 안에 있는지 확인하려고 부른다
		VideoType.fromContentType(storedVideo.contentType());
		VideoFileSize.of(storedVideo.sizeBytes());

		return new VerifiedVideoResult(key.value(), videoStorage.publicUrlOf(key), storedVideo.contentType(),
				storedVideo.sizeBytes());
	}
}
