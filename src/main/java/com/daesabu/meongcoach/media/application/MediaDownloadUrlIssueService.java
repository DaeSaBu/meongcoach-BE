package com.daesabu.meongcoach.media.application;

import com.daesabu.meongcoach.media.application.provided.MediaDownloadUrlIssuer;
import com.daesabu.meongcoach.media.application.required.GcsStorage;
import com.daesabu.meongcoach.media.domain.MediaObjectKey;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MediaDownloadUrlIssueService implements MediaDownloadUrlIssuer {

	private final GcsStorage gcsStorage;

	@Override
	public String issue(MediaScope scope, String objectKey) {
		MediaObjectKey key = MediaObjectKey.parse(scope, objectKey);
		return gcsStorage.issueDownloadUrl(key);
	}
}
