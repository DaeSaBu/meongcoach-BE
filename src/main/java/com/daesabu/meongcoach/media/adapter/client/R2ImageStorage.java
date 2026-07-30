package com.daesabu.meongcoach.media.adapter.client;

import com.daesabu.meongcoach.media.application.required.ImageStorage;
import com.daesabu.meongcoach.media.application.required.ImageUploadUrl;
import org.springframework.stereotype.Component;

/**
 * Cloudflare R2에 대한 이미지 스토리지 어댑터. R2는 S3 호환 API를 제공하므로
 * AWS SDK의 S3Presigner로 presigned PUT URL을 발급한다. presign은 로컬 서명 연산이라 네트워크 호출이 없다.
 */
@Component
public class R2ImageStorage implements ImageStorage {

	private final R2Properties properties;

	public R2ImageStorage(R2Properties properties) {
		this.properties = properties;
	}

	@Override
	public ImageUploadUrl issueUploadUrl(String key, String contentType) {
		throw new UnsupportedOperationException("아직 구현되지 않았다");
	}

	@Override
	public boolean isPublicUrl(String url) {
		throw new UnsupportedOperationException("아직 구현되지 않았다");
	}
}
