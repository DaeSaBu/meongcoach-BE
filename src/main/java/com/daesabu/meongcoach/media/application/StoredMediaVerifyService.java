package com.daesabu.meongcoach.media.application;

import com.daesabu.meongcoach.media.application.provided.StoredMediaVerifier;
import com.daesabu.meongcoach.media.application.required.R2Storage;
import com.daesabu.meongcoach.media.domain.MediaObjectKey;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoredMediaVerifyService implements StoredMediaVerifier {

	private final R2Storage r2Storage;

	@Override
	public void validateR2PublicUrl(MediaScope scope, Long userId, String url) {
		if (url == null || url.isBlank()) {
			return;
		}
		String publicUrlPrefix = r2Storage.resolvePublicUrlPrefix();
		MediaObjectKey.validatePublicUrl(publicUrlPrefix, url, scope, userId);
	}

	@Override
	public boolean isOwnedObjectKey(MediaScope scope, Long userId, String objectKey) {
		if (objectKey == null) {
			return false;
		}
		return new MediaObjectKey(objectKey).belongsTo(scope, userId);
	}
}
