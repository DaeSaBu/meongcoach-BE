package com.daesabu.meongcoach.media.adapter.integration;

import com.daesabu.meongcoach.media.application.required.AnalysisObjectStorage;
import com.daesabu.meongcoach.media.application.required.StorageUploadUrl;
import com.daesabu.meongcoach.media.domain.MediaObjectKey;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.SignUrlOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class GcsObjectStorage implements AnalysisObjectStorage {

	private static final String PUBLIC_BASE_URL = "https://storage.googleapis.com/";
	private static final String STORAGE_URI_SCHEME = "gs://";
	private static final String CONTENT_TYPE_HEADER = "content-type";
	private static final String CONTENT_LENGTH_HEADER = "content-length";

	private final GcsProperties properties;
	private final Storage storage;

	public GcsObjectStorage(GcsProperties properties, Storage storage) {
		this.properties = properties;
		this.storage = storage;
	}

	@Override
	public StorageUploadUrl issueUploadUrl(MediaObjectKey key, String contentType, Long contentLength) {
		BlobInfo blobInfo = BlobInfo.newBuilder(blobId(key)).build();
		Map<String, String> signedHeaders = new HashMap<>();
		signedHeaders.put(CONTENT_TYPE_HEADER, contentType);
		if (contentLength != null) {
			signedHeaders.put(CONTENT_LENGTH_HEADER, String.valueOf(contentLength));
		}
		List<SignUrlOption> options = List.of(
				SignUrlOption.withV4Signature(),
				SignUrlOption.httpMethod(HttpMethod.PUT),
				SignUrlOption.withExtHeaders(signedHeaders));
		String uploadUrl = sign(blobInfo, properties.uploadUrlValidity().toSeconds(), options);
		String publicUrl = PUBLIC_BASE_URL + properties.bucket() + "/" + key.value();
		return new StorageUploadUrl(uploadUrl, publicUrl, properties.uploadUrlValidity().toSeconds());
	}

	@Override
	public String issueDownloadUrl(MediaObjectKey key) {
		BlobInfo blobInfo = BlobInfo.newBuilder(blobId(key)).build();
		List<SignUrlOption> options = List.of(
				SignUrlOption.withV4Signature(),
				SignUrlOption.httpMethod(HttpMethod.GET));
		return sign(blobInfo, properties.downloadUrlValidity().toSeconds(), options);
	}

	@Override
	public String storageUri(MediaObjectKey key) {
		return STORAGE_URI_SCHEME + properties.bucket() + "/" + key.value();
	}

	private BlobId blobId(MediaObjectKey key) {
		return BlobId.of(properties.bucket(), key.value());
	}

	private String sign(BlobInfo blobInfo, long validitySeconds, List<SignUrlOption> options) {
		return storage.signUrl(blobInfo, validitySeconds, TimeUnit.SECONDS, options.toArray(SignUrlOption[]::new))
				.toString();
	}
}
