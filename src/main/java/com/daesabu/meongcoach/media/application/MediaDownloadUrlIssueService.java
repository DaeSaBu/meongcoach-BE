package com.daesabu.meongcoach.media.application;

import com.daesabu.meongcoach.media.application.provided.MediaDownloadUrlIssuer;
import com.daesabu.meongcoach.media.application.required.PrivateObjectStorage;
import com.daesabu.meongcoach.media.domain.MediaObjectKey;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MediaDownloadUrlIssueService implements MediaDownloadUrlIssuer {

	private final PrivateObjectStorage privateObjectStorage;

	@Override
	public String issue(MediaScope scope, String objectKey) {
		MediaObjectKey key = MediaObjectKey.parse(scope, objectKey);
		return privateObjectStorage.issueDownloadUrl(key);
	}
}
