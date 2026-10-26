package com.daesabu.meongcoach.media.application;

import com.daesabu.meongcoach.media.application.provided.StoredMediaVerifier;
import com.daesabu.meongcoach.media.application.required.PublicObjectStorage;
import com.daesabu.meongcoach.media.domain.MediaObjectKey;
import com.daesabu.meongcoach.media.domain.exception.InvalidMediaUrlException;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoredMediaVerifyService implements StoredMediaVerifier {

	private final PublicObjectStorage publicObjectStorage;

	@Override
	public void validateUrl(MediaScope scope, Long userId, String url) {
		if (url == null || url.isBlank()) {
			return;
		}
		boolean owned = publicObjectStorage.objectKeyOf(url)
				.filter(key -> key.belongsTo(scope, userId))
				.isPresent();
		if (!owned) {
			throw new InvalidMediaUrlException();
		}
	}

	@Override
	public boolean isOwnedObjectKey(MediaScope scope, Long userId, String objectKey) {
		if (objectKey == null) {
			return false;
		}
		return new MediaObjectKey(objectKey).belongsTo(scope, userId);
	}
}
