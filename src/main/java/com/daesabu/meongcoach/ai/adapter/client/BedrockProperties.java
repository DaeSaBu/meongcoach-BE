package com.daesabu.meongcoach.ai.adapter.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 영상 분석에 쓰는 AWS Bedrock Converse 연동 설정. SQS·S3와 별개의 자격 증명이라 접두사를 따로 둔다.
 * 설정 오류는 모두 기동 시점에 드러나야 하므로 모든 필드에 제약을 건다.
 *
 * @param region          Bedrock을 호출할 리전. Converse가 s3 URI로 영상을 읽으므로 영상 버킷과 같은 리전이어야 한다
 * @param accessKeyId     Bedrock 호출 IAM 사용자의 액세스 키 ID
 * @param secretAccessKey Bedrock 호출 IAM 사용자의 시크릿 액세스 키
 * @param model           호출할 모델 ID. 영상 입력은 Nova 계열만 지원한다
 * @param responseTimeout 모델 응답을 기다리는 시간. 영상 분석은 수십 초가 걸려 SDK 기본값(30초)으로는 짧다
 */
@Validated
@ConfigurationProperties("meongcoach.ai.bedrock")
public record BedrockProperties(
		@NotBlank String region,
		@NotBlank String accessKeyId,
		@NotBlank String secretAccessKey,
		@NotBlank String model,
		@NotNull Duration responseTimeout
) {
}
