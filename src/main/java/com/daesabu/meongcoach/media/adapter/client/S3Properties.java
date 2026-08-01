package com.daesabu.meongcoach.media.adapter.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 훈련 영상을 보관하는 AWS S3 연동 설정. R2와 달리 엔드포인트가 아니라 리전으로 주소가 결정된다.
 * 설정 오류는 모두 기동 시점에 드러나야 하므로 모든 필드에 제약을 건다.
 *
 * @param region              버킷이 있는 리전. 예: ap-northeast-2
 * @param accessKeyId         S3 접근 IAM 사용자의 액세스 키 ID
 * @param secretAccessKey     S3 접근 IAM 사용자의 시크릿 액세스 키
 * @param bucket              영상을 담는 버킷 이름. 가상 호스팅 URL을 쓰므로 점이 없는 DNS 호환 이름이어야 한다
 * @param publicBaseUrl       버킷의 공개 도메인(CDN 또는 S3 표준 URL). 끝에 슬래시를 붙이지 않는다
 * @param uploadUrlValidity   업로드 presigned URL의 유효 시간
 * @param downloadUrlValidity 다운로드 presigned URL의 유효 시간
 */
@Validated
@ConfigurationProperties("meongcoach.storage.s3")
public record S3Properties(
		@NotBlank String region,
		@NotBlank String accessKeyId,
		@NotBlank String secretAccessKey,
		@NotBlank String bucket,
		@NotBlank String publicBaseUrl,
		@NotNull Duration uploadUrlValidity,
		@NotNull Duration downloadUrlValidity
) {
}
