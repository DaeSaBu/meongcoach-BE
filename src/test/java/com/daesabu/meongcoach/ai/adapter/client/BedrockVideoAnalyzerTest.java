package com.daesabu.meongcoach.ai.adapter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.ServiceTierType;
import software.amazon.awssdk.services.bedrockruntime.model.VideoFormat;

/**
 * 모델 호출은 mock 클라이언트로 가로채고, Converse 요청 구성과 응답 처리를 검증한다.
 */
@DisplayName("Bedrock 영상 분석 어댑터")
class BedrockVideoAnalyzerTest {

	private static final String S3_URI = "s3://test-video-bucket/videos/training/7/key.mp4";
	private static final String MODEL_ID = "test.nova-video-v1:0";

	private BedrockRuntimeClient client;
	private BedrockVideoAnalyzer analyzer;

	@BeforeEach
	void setUp() {
		client = mock(BedrockRuntimeClient.class);
		analyzer = new BedrockVideoAnalyzer(client, propertiesWithPromptVersion("v1"));
	}

	private BedrockProperties propertiesWithPromptVersion(String promptVersion) {
		return new BedrockProperties(
				"ap-northeast-2", "test-access-key", "test-secret-key", MODEL_ID, Duration.ofMinutes(5),
				ServiceTierType.FLEX, 4096, 0.2f, promptVersion);
	}

	private void givenModelResponds(String content) {
		when(client.converse(any(ConverseRequest.class))).thenReturn(ConverseResponse.builder()
				.output(ConverseOutput.fromMessage(Message.builder()
						.role(ConversationRole.ASSISTANT)
						.content(ContentBlock.fromText(content))
						.build()))
				.build());
	}

	private ConverseRequest capturedRequest() {
		ArgumentCaptor<ConverseRequest> captor = ArgumentCaptor.forClass(ConverseRequest.class);
		verify(client).converse(captor.capture());
		return captor.getValue();
	}

	@Test
	@DisplayName("모델 응답을 분석 결과로 반환한다")
	void analyzeReturnsModelResponse() {
		givenModelResponds("분리불안 징후가 관찰됩니다.");

		String content = analyzer.analyze(S3_URI);

		assertThat(content).isEqualTo("분리불안 징후가 관찰됩니다.");
	}

	@Test
	@DisplayName("설정된 모델 ID로 요청한다")
	void analyzeUsesConfiguredModelId() {
		givenModelResponds("결과");

		analyzer.analyze(S3_URI);

		assertThat(capturedRequest().modelId()).isEqualTo(MODEL_ID);
	}

	@Test
	@DisplayName("설정된 서비스 등급으로 요청한다")
	void analyzeUsesConfiguredServiceTier() {
		givenModelResponds("결과");

		analyzer.analyze(S3_URI);

		assertThat(capturedRequest().serviceTier().type()).isEqualTo(ServiceTierType.FLEX);
	}

	@Test
	@DisplayName("설정된 최대 토큰·온도로 요청한다")
	void analyzeUsesConfiguredInferenceConfig() {
		givenModelResponds("결과");

		analyzer.analyze(S3_URI);

		assertThat(capturedRequest().inferenceConfig().maxTokens()).isEqualTo(4096);
		assertThat(capturedRequest().inferenceConfig().temperature()).isEqualTo(0.2f);
	}

	@Test
	@DisplayName("비디오 블록을 텍스트보다 먼저 보낸다")
	void analyzeSendsVideoBeforeText() {
		givenModelResponds("결과");

		analyzer.analyze(S3_URI);

		// 순서가 뒤집히면 Nova가 지시를 무시하고 영어 장면 묘사로 빠진다
		List<ContentBlock> contents = capturedRequest().messages().getFirst().content();
		assertThat(contents.getFirst().video()).isNotNull();
		assertThat(contents.get(1).text()).isNotNull();
	}

	@Test
	@DisplayName("s3 URI를 비디오 s3Location으로 실어 보낸다")
	void analyzeAttachesS3UriAsS3Location() {
		givenModelResponds("결과");

		analyzer.analyze(S3_URI);

		// presigned URL을 보내면 Bedrock이 s3 위치로 해석해 거부한다
		ContentBlock video = capturedRequest().messages().getFirst().content().getFirst();
		assertThat(video.video().source().s3Location().uri()).isEqualTo(S3_URI);
	}

	@Test
	@DisplayName("mp4 영상은 mp4 형식으로 보낸다")
	void analyzeUsesMp4FormatForMp4() {
		givenModelResponds("결과");

		analyzer.analyze(S3_URI);

		ContentBlock video = capturedRequest().messages().getFirst().content().getFirst();
		assertThat(video.video().format()).isEqualTo(VideoFormat.MP4);
	}

	@Test
	@DisplayName("mov 영상은 mov 형식으로 보낸다")
	void analyzeUsesMovFormatForMov() {
		givenModelResponds("결과");

		analyzer.analyze("s3://test-video-bucket/videos/training/7/key.mov");

		ContentBlock video = capturedRequest().messages().getFirst().content().getFirst();
		assertThat(video.video().format()).isEqualTo(VideoFormat.MOV);
	}

	@Test
	@DisplayName("분석 지시를 system 메시지로 보낸다")
	void analyzeSendsAnalysisInstructionAsSystemMessage() {
		givenModelResponds("결과");

		analyzer.analyze(S3_URI);

		assertThat(capturedRequest().system().getFirst().text()).contains("반려견");
	}

	@Test
	@DisplayName("없는 프롬프트 버전이면 생성에 실패한다")
	void constructorFailsWhenPromptVersionIsUnknown() {
		assertThatThrownBy(() -> new BedrockVideoAnalyzer(client, propertiesWithPromptVersion("v999")))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("모델 응답이 비어 있으면 분석에 실패한다")
	void analyzeFailsWhenResponseIsBlank() {
		givenModelResponds(" ");

		assertThatThrownBy(() -> analyzer.analyze(S3_URI))
				.isInstanceOf(IllegalStateException.class);
	}
}
