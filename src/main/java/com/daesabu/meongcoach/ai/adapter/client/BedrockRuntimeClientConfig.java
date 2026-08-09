package com.daesabu.meongcoach.ai.adapter.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

/**
 * 영상 분석에 쓸 Bedrock Converse 클라이언트를 등록한다.
 * 자격 증명·리전은 기본 프로바이더 체인에 맡기지 않고 전용 프로퍼티로 명시해 주입한다.
 * 기본 체인은 spring-cloud-aws(SQS)와 같은 환경 변수를 읽어 서로의 설정을 가져갈 수 있기 때문이다.
 */
@Configuration
public class BedrockRuntimeClientConfig {

	@Bean
	public BedrockRuntimeClient bedrockRuntimeClient(BedrockProperties properties) {
		return BedrockRuntimeClient.builder()
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())))
				.region(Region.of(properties.region()))
				// 영상 분석은 수십 초 이상 걸릴 수 있어 SDK 기본 타임아웃 대신 전용 값을 준다
				.overrideConfiguration(override -> override
						.apiCallTimeout(properties.responseTimeout())
						.apiCallAttemptTimeout(properties.responseTimeout()))
				.build();
	}
}
