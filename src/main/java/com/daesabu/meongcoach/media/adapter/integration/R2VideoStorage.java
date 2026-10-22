package com.daesabu.meongcoach.media.adapter.integration;

import com.daesabu.meongcoach.media.application.required.PublicVideoStorage;
import com.daesabu.meongcoach.media.application.required.VideoUploadUrl;
import com.daesabu.meongcoach.media.domain.VideoObjectKey;
import java.net.URI;
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
public class R2VideoStorage implements PublicVideoStorage {

	private final R2Properties properties;
	private final String publicBaseUrl;
	private final S3Presigner presigner;

	public R2VideoStorage(R2Properties properties) {
		this.properties = properties;
		this.publicBaseUrl = trimTrailingSlash(properties.publicBaseUrl());
		this.presigner = S3Presigner.builder()
				.endpointOverride(URI.create(properties.endpoint()))
				.region(Region.of("auto"))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())))
				.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
				.build();
	}

	@Override
	public VideoUploadUrl issueUploadUrl(VideoObjectKey key, String contentType, long contentLength) {
		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(properties.bucket())
				.key(key.value())
				.contentType(contentType)
				.contentLength(contentLength)
				.build();
		PresignedPutObjectRequest presigned = presigner.presignPutObject(PutObjectPresignRequest.builder()
				.signatureDuration(properties.uploadUrlValidity())
				.putObjectRequest(putObjectRequest)
				.build());
		return new VideoUploadUrl(presigned.url().toString(), publicBaseUrl + "/" + key.value(), key.value(),
				properties.uploadUrlValidity().toSeconds());
	}

	private static String trimTrailingSlash(String url) {
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}
}
