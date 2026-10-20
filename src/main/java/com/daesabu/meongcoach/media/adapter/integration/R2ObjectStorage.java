package com.daesabu.meongcoach.media.adapter.integration;

import com.daesabu.meongcoach.media.application.required.PublicObjectStorage;
import com.daesabu.meongcoach.media.application.required.StorageUploadUrl;
import com.daesabu.meongcoach.media.domain.MediaObjectKey;
import java.net.URI;
import java.util.Optional;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
public class R2ObjectStorage implements PublicObjectStorage {

	private final R2Properties properties;
	private final String publicUrlPrefix;
	private final S3Presigner presigner;

	public R2ObjectStorage(R2Properties properties) {
		this.properties = properties;
		this.publicUrlPrefix = StorageUrls.trimTrailingSlash(properties.publicBaseUrl()) + "/";
		this.presigner = S3Presigner.builder()
				.endpointOverride(URI.create(properties.endpoint()))
				.region(Region.of("auto"))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())))
				.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
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
	public Optional<MediaObjectKey> objectKeyOf(String url) {
		if (url == null || !url.startsWith(publicUrlPrefix)) {
			return Optional.empty();
		}
		return Optional.of(new MediaObjectKey(url.substring(publicUrlPrefix.length())));
	}
}
