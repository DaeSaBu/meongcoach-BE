package com.daesabu.meongcoach.media.adapter.integration;

import com.daesabu.meongcoach.media.application.required.ImageStorage;
import com.daesabu.meongcoach.media.application.required.ImageUploadUrl;
import com.daesabu.meongcoach.media.domain.vo.ImageObjectKey;
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
 * Cloudflare R2에 대한 이미지 스토리지 어댑터. R2는 S3 호환 API를 제공하므로
 * AWS SDK의 S3Presigner로 presigned PUT URL을 발급한다. presign은 로컬 서명 연산이라 네트워크 호출이 없다.
 */
@Component
public class R2ImageStorage implements ImageStorage {

	private final R2Properties properties;
	private final String publicBaseUrl;
	private final S3Presigner presigner;

	public R2ImageStorage(R2Properties properties) {
		this.properties = properties;
		// 프리픽스 판별에 쓰이므로 끝 슬래시를 미리 정리해 둔다
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
	public ImageUploadUrl issueUploadUrl(ImageObjectKey key, String contentType) {
		// Content-Type을 서명에 포함해 발급 시 지정한 형식으로만 업로드되게 한다
		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(properties.bucket())
				.key(key.value())
				.contentType(contentType)
				.build();
		PresignedPutObjectRequest presigned = presigner.presignPutObject(PutObjectPresignRequest.builder()
				.signatureDuration(properties.uploadUrlValidity())
				.putObjectRequest(putObjectRequest)
				.build());
		return new ImageUploadUrl(presigned.url().toString(), publicBaseUrl + "/" + key.value(),
				properties.uploadUrlValidity().toSeconds());
	}

	@Override
	public boolean isPublicUrl(String url) {
		// 도메인 뒤에 다른 호스트를 이어 붙인 위장 URL을 막기 위해 구분자까지 포함해 비교한다
		return url != null && url.startsWith(publicBaseUrl + "/");
	}

	private static String trimTrailingSlash(String url) {
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}
}
