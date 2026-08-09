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
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.S3Location;
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

	// 영상 입력 시 모델이 지시를 무시하고 영어 장면 묘사로 빠지는 경향이 있어, 지시는 system 메시지로 분리해
	// 한국어와 4개 항목 구조를 규칙과 출력 형식으로 강제한다
	private static final String SYSTEM_PROMPT = """
			당신은 반려견 행동 전문가입니다. 사용자가 첨부한 반려견 영상을 분석해 보호자에게 전달할 행동 분석 리포트를 작성합니다.

			반드시 아래 규칙을 지키세요.
			- 모든 문장을 한국어로만 작성합니다. 영어 문장을 섞지 않습니다.
			- 장면을 시간 순으로 나열하는 묘사문이 아니라, 아래 4개 항목으로 종합한 리포트를 작성합니다.
			- 아래 출력 형식의 제목 4개를 순서와 문구 그대로 사용하고, 다른 항목을 추가하지 않습니다.
			- 보호자가 이해하기 쉬운 표현을 쓰고, 영상에서 확인되지 않는 내용은 추측하지 않습니다.

			출력 형식:
			## 1. 주요 행동 관찰
			(영상 속 반려견의 주요 행동 관찰 내용)

			## 2. 감정 상태와 신호
			(행동에서 읽을 수 있는 감정 상태와 신호)

			## 3. 문제 행동 징후
			(주의가 필요한 문제 행동 징후. 없으면 "특이 징후 없음"이라고 명시)

			## 4. 훈련·개선 제안
			(보호자가 실천할 수 있는 훈련·개선 제안)
			""";

	private static final String USER_PROMPT = "첨부한 반려견 영상을 규칙에 따라 분석해 한국어 리포트를 작성하세요.";

	private final BedrockRuntimeClient bedrockRuntimeClient;
	private final String modelId;

	public BedrockVideoAnalyzer(BedrockRuntimeClient bedrockRuntimeClient, BedrockProperties properties) {
		this.bedrockRuntimeClient = bedrockRuntimeClient;
		this.modelId = properties.model();
	}

	@Override
	public String analyze(String videoS3Uri) {
		ConverseRequest request = ConverseRequest.builder()
				.modelId(modelId)
				.system(SystemContentBlock.fromText(SYSTEM_PROMPT))
				.messages(Message.builder()
						.role(ConversationRole.USER)
						.content(
								ContentBlock.fromVideo(VideoBlock.builder()
										.format(videoFormatOf(videoS3Uri))
										.source(VideoSource.fromS3Location(
												S3Location.builder().uri(videoS3Uri).build()))
										.build()),
								ContentBlock.fromText(USER_PROMPT))
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
