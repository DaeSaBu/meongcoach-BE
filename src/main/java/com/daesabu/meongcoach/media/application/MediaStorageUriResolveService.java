package com.daesabu.meongcoach.media.application;

import com.daesabu.meongcoach.media.application.provided.MediaStorageUriResolver;
import com.daesabu.meongcoach.media.application.required.GcsStorage;
import com.daesabu.meongcoach.media.domain.MediaObjectKey;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MediaStorageUriResolveService implements MediaStorageUriResolver {

	private final GcsStorage gcsStorage;

	@Override
	public String resolve(MediaScope scope, String objectKey) {
		MediaObjectKey key = MediaObjectKey.parse(scope, objectKey);
		return gcsStorage.storageUri(key);
	}
}
