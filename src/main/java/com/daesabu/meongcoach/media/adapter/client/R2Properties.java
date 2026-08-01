package com.daesabu.meongcoach.media.adapter.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cloudflare R2 연동 설정. R2는 S3 호환 API라 presigned URL 발급에 AWS SDK를 그대로 쓴다.
 * 설정 오류는 모두 기동 시점에 드러나야 하므로 모든 필드에 제약을 건다.
 *
 * @param endpoint        S3 호환 API 엔드포인트. https://{계정ID}.r2.cloudflarestorage.com
 * @param accessKeyId     R2 API 토큰의 액세스 키 ID
 * @param secretAccessKey R2 API 토큰의 시크릿 액세스 키
 * @param bucket          이미지와 영상을 함께 담는 버킷 이름
 * @param publicBaseUrl   버킷에 연결된 공개 도메인(커스텀 도메인 또는 r2.dev). 끝에 슬래시를 붙이지 않는다
 * @param uploadUrlValidity      이미지 presigned URL의 유효 시간
 * @param videoUploadUrlValidity 영상 presigned URL의 유효 시간. 영상은 최대 100MB라 업로드가 이미지보다 오래 걸려 따로 둔다
 */
@Validated
@ConfigurationProperties("meongcoach.storage.r2")
public record R2Properties(
		@NotBlank String endpoint,
		@NotBlank String accessKeyId,
		@NotBlank String secretAccessKey,
		@NotBlank String bucket,
		@NotBlank String publicBaseUrl,
		@NotNull Duration uploadUrlValidity,
		@NotNull Duration videoUploadUrlValidity
) {
}
