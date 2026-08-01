package com.daesabu.meongcoach.media.adapter.client;

import com.daesabu.meongcoach.media.application.required.StoredVideo;
import com.daesabu.meongcoach.media.application.required.VideoStorage;
import com.daesabu.meongcoach.media.application.required.VideoUploadUrl;
import com.daesabu.meongcoach.media.domain.vo.VideoObjectKey;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Cloudflare R2에 대한 영상 스토리지 어댑터. R2는 S3 호환 API를 제공하므로
 * AWS SDK의 S3Presigner로 presigned PUT URL을 발급하고, S3Client로 저장된 객체를 조회한다.
 * presign은 로컬 서명 연산이라 네트워크 호출이 없지만 객체 조회(HeadObject)는 실제로 R2를 호출한다.
 * presigner 구성은 R2ImageStorage와 같지만 유효 시간이 달라 이미지 경로를 건드리지 않으려고 따로 둔다.
 */
@Component
public class R2VideoStorage implements VideoStorage {

	// R2가 응답하지 않을 때 요청 스레드가 무한정 묶이지 않도록 SDK 기본값(무제한) 대신 짧은 상한을 명시한다
	private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(2);
	private static final Duration API_CALL_TIMEOUT = Duration.ofSeconds(3);
	private static final int STATUS_FORBIDDEN = 403;
	private static final int STATUS_NOT_FOUND = 404;

	private final R2Properties properties;
	private final String publicBaseUrl;
	private final S3Presigner presigner;
	private final S3Client s3Client;

	// 생성자가 둘이라 주입 대상을 명시한다
	@Autowired
	public R2VideoStorage(R2Properties properties) {
		this(properties, buildS3Client(properties));
	}

	// HeadObject 응답을 테스트에서 가로챌 수 있도록 S3Client를 직접 받는 통로를 둔다
	R2VideoStorage(R2Properties properties, S3Client s3Client) {
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
		this.s3Client = s3Client;
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
		return new VideoUploadUrl(presigned.url().toString(), publicUrlOf(key),
				properties.videoUploadUrlValidity().toSeconds());
	}

	@Override
	public Optional<StoredVideo> findStoredVideo(VideoObjectKey key) {
		try {
			HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
					.bucket(properties.bucket())
					.key(key.value())
					.build());
			return Optional.of(new StoredVideo(response.contentType(), response.contentLength()));
		} catch (NoSuchKeyException e) {
			return Optional.empty();
		} catch (S3Exception e) {
			// R2는 토큰 권한에 따라 없는 객체에 404 대신 403을 주기도 해서 둘 다 "없음"으로 본다
			if (e.statusCode() == STATUS_NOT_FOUND || e.statusCode() == STATUS_FORBIDDEN) {
				return Optional.empty();
			}
			// 그 밖의 오류는 업로드 여부를 판단할 근거가 못 되므로 전역 핸들러가 500으로 다루도록 그대로 올려보낸다
			throw e;
		}
	}

	@Override
	public String publicUrlOf(VideoObjectKey key) {
		return publicBaseUrl + "/" + key.value();
	}

	private static S3Client buildS3Client(R2Properties properties) {
		return S3Client.builder()
				.endpointOverride(URI.create(properties.endpoint()))
				.region(Region.of("auto"))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())))
				.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
				.httpClientBuilder(Apache5HttpClient.builder().connectionTimeout(CONNECTION_TIMEOUT))
				.overrideConfiguration(ClientOverrideConfiguration.builder()
						.apiCallTimeout(API_CALL_TIMEOUT)
						.build())
				.build();
	}

	private static String trimTrailingSlash(String url) {
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}
}
