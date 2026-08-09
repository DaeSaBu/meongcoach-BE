package com.daesabu.meongcoach.ai.adapter.client;

import com.daesabu.meongcoach.ai.application.required.VideoAnalyzer;
import java.net.URI;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.S3Location;
import software.amazon.awssdk.services.bedrockruntime.model.ServiceTier;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.VideoBlock;
import software.amazon.awssdk.services.bedrockruntime.model.VideoFormat;
import software.amazon.awssdk.services.bedrockruntime.model.VideoSource;

/**
 * AWS Bedrock Converse로 영상을 분석하는 어댑터. 영상은 바이트로 내려받지 않고 s3://버킷/키 URI를 넘겨
 * Bedrock이 버킷에서 직접 읽게 하며, 그래서 호출 자격 증명에 영상 버킷의 s3:GetObject 권한이 필요하다.
 * 사용자 메시지는 반드시 비디오를 텍스트보다 먼저 둔다. 순서가 뒤집히면 Nova가 지시(언어·출력 형식)를
 * 무시하고 영어 장면 묘사로 빠진다. Spring AI는 이 순서를 강제할 수 없어 SDK를 직접 쓴다.
 * 실패는 그대로 전파하고, 삼킬지는 호출부인 AiReportGenerateService가 정한다.
 */
@Component
public class BedrockVideoAnalyzer implements VideoAnalyzer {

	private static final String PROMPT_BASE = "prompts/video-analysis/";

	private final BedrockRuntimeClient bedrockRuntimeClient;
	private final String modelId;
	private final ServiceTier serviceTier;
	private final InferenceConfiguration inferenceConfig;
	// 영상 입력 시 모델이 지시를 무시하고 영어 장면 묘사로 빠지는 경향이 있어, 지시는 system 메시지로 분리해
	// 한국어와 4개 항목 구조를 규칙과 출력 형식으로 강제한다. 내용은 resources/prompts/video-analysis 참고
	private final String systemPrompt;
	private final String userPrompt;

	public BedrockVideoAnalyzer(BedrockRuntimeClient bedrockRuntimeClient, BedrockProperties properties) {
		this.bedrockRuntimeClient = bedrockRuntimeClient;
		this.modelId = properties.model();
		this.serviceTier = ServiceTier.builder().type(properties.serviceTier()).build();
		this.inferenceConfig = InferenceConfiguration.builder()
				.maxTokens(properties.maxTokens())
				.temperature(properties.temperature())
				.build();
		String promptDir = PROMPT_BASE + properties.promptVersion() + "/";
		this.systemPrompt = PromptLoader.load(promptDir + "system.md");
		this.userPrompt = PromptLoader.load(promptDir + "user.md");
	}

	@Override
	public String analyze(String videoS3Uri) {
		ConverseRequest request = ConverseRequest.builder()
				.modelId(modelId)
				.serviceTier(serviceTier)
				.inferenceConfig(inferenceConfig)
				.system(SystemContentBlock.fromText(systemPrompt))
				.messages(Message.builder()
						.role(ConversationRole.USER)
						.content(
								ContentBlock.fromVideo(VideoBlock.builder()
										.format(videoFormatOf(videoS3Uri))
										.source(VideoSource.fromS3Location(
												S3Location.builder().uri(videoS3Uri).build()))
										.build()),
								ContentBlock.fromText(userPrompt))
						.build())
				.build();

		ConverseResponse response = bedrockRuntimeClient.converse(request);

		String content = response.output().message().content().stream()
				.map(ContentBlock::text)
				.filter(Objects::nonNull)
				.collect(Collectors.joining());
		if (content.isBlank()) {
			throw new IllegalStateException("영상 분석 결과가 비어 있습니다: " + videoS3Uri);
		}
		return content;
	}

	private static VideoFormat videoFormatOf(String videoUri) {
		String path = URI.create(videoUri).getPath();
		if (path != null && path.endsWith(".mov")) {
			return VideoFormat.MOV;
		}
		return VideoFormat.MP4;
	}
}
