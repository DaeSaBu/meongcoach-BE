package com.daesabu.meongcoach.media.adapter.integration;

import com.daesabu.meongcoach.media.application.required.PrivateObjectStorage;
import com.daesabu.meongcoach.media.application.required.StorageUploadUrl;
import com.daesabu.meongcoach.media.domain.MediaObjectKey;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
public class S3ObjectStorage implements PrivateObjectStorage {

	private final S3Properties properties;
	private final String publicUrlPrefix;
	private final S3Presigner presigner;

	public S3ObjectStorage(S3Properties properties) {
		this.properties = properties;
		this.publicUrlPrefix = StorageUrls.trimTrailingSlash(properties.publicBaseUrl()) + "/";
		this.presigner = S3Presigner.builder()
				.region(Region.of(properties.region()))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())))
				.build();
	}

	@Override
	public StorageUploadUrl issueUploadUrl(MediaObjectKey key, String contentType, Long contentLength) {
		PutObjectRequest putObjectRequest =
				StorageUrls.putObjectRequest(properties.bucket(), key, contentType, contentLength);
		PresignedPutObjectRequest presigned = presigner.presignPutObject(PutObjectPresignRequest.builder()
				.signatureDuration(properties.uploadUrlValidity())
				.putObjectRequest(putObjectRequest)
				.build());
		return new StorageUploadUrl(presigned.url().toString(), publicUrlPrefix + key.value(),
				properties.uploadUrlValidity().toSeconds());
	}

	@Override
	public String issueDownloadUrl(MediaObjectKey key) {
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(properties.bucket())
				.key(key.value())
				.build();
		PresignedGetObjectRequest presigned = presigner.presignGetObject(GetObjectPresignRequest.builder()
				.signatureDuration(properties.downloadUrlValidity())
				.getObjectRequest(getObjectRequest)
				.build());
		return presigned.url().toString();
	}
}
