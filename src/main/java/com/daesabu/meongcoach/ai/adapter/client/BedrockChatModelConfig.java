package com.daesabu.meongcoach.ai.adapter.client;

import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/**
 * 영상 분석에 쓸 Bedrock Converse 모델을 등록한다.
 * Bedrock 스타터를 쓰지 않는 이유는 스타터가 등록하는 자격 증명·리전 빈이 spring-cloud-aws(SQS)의
 * 동일 타입 빈과 경쟁해 서로의 설정을 가져갈 수 있기 때문이다. 자격 증명은 전용 프로퍼티로 명시해 주입한다.
 */
@Configuration
public class BedrockChatModelConfig {

	@Bean
	public BedrockProxyChatModel bedrockProxyChatModel(BedrockProperties properties) {
		return BedrockProxyChatModel.builder()
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())))
				.region(Region.of(properties.region()))
				.options(BedrockChatOptions.builder().model(properties.model()).build())
				.timeout(properties.responseTimeout())
				.socketTimeout(properties.responseTimeout())
				.build();
	}
}
