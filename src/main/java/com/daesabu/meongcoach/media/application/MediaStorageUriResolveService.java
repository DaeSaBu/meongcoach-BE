package com.daesabu.meongcoach.media.application;

import com.daesabu.meongcoach.media.application.provided.MediaStorageUriResolver;
import com.daesabu.meongcoach.media.application.required.AnalysisObjectStorage;
import com.daesabu.meongcoach.media.domain.MediaObjectKey;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import com.daesabu.meongcoach.media.domain.shared.MediaVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MediaStorageUriResolveService implements MediaStorageUriResolver {

	private final AnalysisObjectStorage analysisObjectStorage;

	@Override
	public String resolve(MediaScope scope, String objectKey) {
		if (scope.visibility() != MediaVisibility.ANALYSIS) {
			throw new IllegalArgumentException("분석 스토리지 범위가 아니다: " + scope.path());
		}
		MediaObjectKey key = MediaObjectKey.parse(scope, objectKey);
		return analysisObjectStorage.storageUri(key);
	}
}
