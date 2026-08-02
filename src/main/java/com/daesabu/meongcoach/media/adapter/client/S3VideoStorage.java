package com.daesabu.meongcoach.media.adapter.client;

import com.daesabu.meongcoach.media.application.required.VideoDownloadUrl;
import com.daesabu.meongcoach.media.application.required.VideoStorage;
import com.daesabu.meongcoach.media.application.required.VideoUploadUrl;
import com.daesabu.meongcoach.media.domain.vo.VideoObjectKey;
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

/**
 * AWS S3에 대한 영상 스토리지 어댑터. presign은 로컬 서명 연산이라 네트워크 호출이 없다.
 * 영상은 최대 100MB라 멀티파트 없이 단일 PUT presigned URL을 발급한다.
 */
@Component
public class S3VideoStorage implements VideoStorage {

	private final S3Properties properties;
	private final String publicBaseUrl;
	private final S3Presigner presigner;

	public S3VideoStorage(S3Properties properties) {
		this.properties = properties;
		this.publicBaseUrl = trimTrailingSlash(properties.publicBaseUrl());
		// 실제 AWS S3라 R2와 달리 엔드포인트·경로 스타일을 지정하지 않고 리전만 주면 가상 호스팅 URL이 만들어진다
		this.presigner = S3Presigner.builder()
				.region(Region.of(properties.region()))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())))
				.build();
	}

	@Override
	public VideoUploadUrl issueUploadUrl(VideoObjectKey key, String contentType, long contentLength) {
		// Content-Type뿐 아니라 Content-Length까지 서명에 넣어 신고한 크기와 다른 업로드를 S3가 거부하게 한다.
		// 대신 클라이언트는 chunked 전송이 아니라 길이를 아는 body로 PUT해야 한다
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
		// 버킷을 비공개로 운영할 수 있어 공개 URL과 함께 객체 키도 돌려준다. 후속 API는 객체 키를 기준으로 삼는다
		return new VideoUploadUrl(presigned.url().toString(), publicBaseUrl + "/" + key.value(), key.value(),
				properties.uploadUrlValidity().toSeconds());
	}

	@Override
	public VideoDownloadUrl issueDownloadUrl(VideoObjectKey key) {
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(properties.bucket())
				.key(key.value())
				.build();
		PresignedGetObjectRequest presigned = presigner.presignGetObject(GetObjectPresignRequest.builder()
				.signatureDuration(properties.downloadUrlValidity())
				.getObjectRequest(getObjectRequest)
				.build());
		return new VideoDownloadUrl(presigned.url().toString(), publicBaseUrl + "/" + key.value(),
				publicBaseUrl + "/" + key.thumbnailKey(), properties.downloadUrlValidity().toSeconds());
	}

	private static String trimTrailingSlash(String url) {
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}
}
