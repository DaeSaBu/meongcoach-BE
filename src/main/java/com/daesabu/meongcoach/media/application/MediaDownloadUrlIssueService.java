package com.daesabu.meongcoach.media.application;

import com.daesabu.meongcoach.media.application.provided.MediaDownloadUrlIssuer;
import com.daesabu.meongcoach.media.application.required.AnalysisObjectStorage;
import com.daesabu.meongcoach.media.application.required.PrivateObjectStorage;
import com.daesabu.meongcoach.media.domain.MediaObjectKey;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import com.daesabu.meongcoach.media.domain.shared.MediaVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MediaDownloadUrlIssueService implements MediaDownloadUrlIssuer {

	private final PrivateObjectStorage privateObjectStorage;
	private final AnalysisObjectStorage analysisObjectStorage;

	@Override
	public String issue(MediaScope scope, String objectKey) {
		MediaObjectKey key = MediaObjectKey.parse(scope, objectKey);
		if (scope.visibility() == MediaVisibility.ANALYSIS) {
			return analysisObjectStorage.issueDownloadUrl(key);
		}
		return privateObjectStorage.issueDownloadUrl(key);
	}
}
