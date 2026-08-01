package com.daesabu.meongcoach.media.adapter.client;

import com.daesabu.meongcoach.media.application.required.VideoStorage;
import com.daesabu.meongcoach.media.application.required.VideoUploadUrl;
import com.daesabu.meongcoach.media.domain.vo.VideoObjectKey;
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

/**
 * Cloudflare R2에 대한 영상 스토리지 어댑터. R2는 S3 호환 API를 제공하므로
 * AWS SDK의 S3Presigner로 presigned PUT URL을 발급한다. presign은 로컬 서명 연산이라 네트워크 호출이 없다.
 * presigner 구성은 R2ImageStorage와 같지만 유효 시간이 달라 이미지 경로를 건드리지 않으려고 따로 둔다.
 */
@Component
public class R2VideoStorage implements VideoStorage {

	private final R2Properties properties;
	private final String publicBaseUrl;
	private final S3Presigner presigner;

	public R2VideoStorage(R2Properties properties) {
		this.properties = properties;
		// 공개 URL을 이어 붙일 때 슬래시가 겹치지 않도록 끝 슬래시를 미리 정리해 둔다
		this.publicBaseUrl = trimTrailingSlash(properties.publicBaseUrl());
		this.presigner = S3Presigner.builder()
				.endpointOverride(URI.create(properties.endpoint()))
				// R2는 리전 개념이 없어 SDK 요구사항을 채우는 auto를 쓴다
				.region(Region.of("auto"))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())))
				// R2 권장 방식인 경로 스타일(<endpoint>/<bucket>/<key>)을 쓴다
				.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
				.build();
	}

	@Override
	public VideoUploadUrl issueUploadUrl(VideoObjectKey key, String contentType, long contentLength) {
		// Content-Type과 Content-Length를 함께 서명에 포함해 발급 시 지정한 형식·크기로만 업로드되게 한다
		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(properties.bucket())
				.key(key.value())
				.contentType(contentType)
				.contentLength(contentLength)
				.build();
		PresignedPutObjectRequest presigned = presigner.presignPutObject(PutObjectPresignRequest.builder()
				.signatureDuration(properties.videoUploadUrlValidity())
				.putObjectRequest(putObjectRequest)
				.build());
		return new VideoUploadUrl(presigned.url().toString(), toPublicUrl(key),
				properties.videoUploadUrlValidity().toSeconds());
	}

	private String toPublicUrl(VideoObjectKey key) {
		return publicBaseUrl + "/" + key.value();
	}

	private static String trimTrailingSlash(String url) {
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}
}
