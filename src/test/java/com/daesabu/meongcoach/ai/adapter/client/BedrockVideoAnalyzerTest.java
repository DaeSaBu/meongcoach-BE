package com.daesabu.meongcoach.ai.adapter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.TopicSummary;
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
import tools.jackson.databind.ObjectMapper;

/**
 * 모델 호출은 mock 클라이언트로 가로채고, Converse 요청 구성과 응답 처리를 검증한다.
 */
@DisplayName("Bedrock 영상 분석 어댑터")
class BedrockVideoAnalyzerTest {

	private static final String S3_URI = "s3://test-video-bucket/videos/training/7/key.mp4";
	private static final String MODEL_ID = "test.nova-video-v1:0";
	// 정규화 재직렬화 결과와 비교할 수 있도록 record 컴포넌트 순서(recommend, report, solution)와 맞춘 JSON
	private static final String VALID_CONTENT_JSON = "{\"recommend\":[{\"title\":\"입질 교정\"}],"
			+ "\"report\":[{\"subTitle\":\"영상에서 이런 행동이 보여요\",\"description\":\"슬리퍼를 물고 달려요.\"}],"
			+ "\"solution\":[{\"order\":1,\"title\":\"교환 놀이 연습하기\",\"description\":\"간식과 바꿔 주세요.\"}]}";

	private BedrockRuntimeClient client;
	private TopicFinder topicFinder;
	private BedrockVideoAnalyzer analyzer;

	@BeforeEach
	void setUp() {
		client = mock(BedrockRuntimeClient.class);
		topicFinder = mock(TopicFinder.class);
		analyzer = new BedrockVideoAnalyzer(client, properties(), topicFinder, new ObjectMapper());
	}

	private BedrockProperties properties() {
		return new BedrockProperties(
				"ap-northeast-2", "test-access-key", "test-secret-key", MODEL_ID, Duration.ofMinutes(5),
				ServiceTierType.FLEX, 4096, 0.2f);
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
	@DisplayName("모델이 반환한 JSON을 정규화해 반환한다")
	void analyzeReturnsNormalizedJson() {
		givenModelResponds(VALID_CONTENT_JSON);

		String content = analyzer.analyze(S3_URI);

		assertThat(content).isEqualTo(VALID_CONTENT_JSON);
	}

	@Test
	@DisplayName("코드 펜스와 앞뒤 설명을 제거하고 JSON만 파싱한다")
	void analyzeStripsFencesAndSurroundingText() {
		givenModelResponds("리포트를 작성했어요.\n```json\n" + VALID_CONTENT_JSON + "\n```\n확인해 주세요.");

		String content = analyzer.analyze(S3_URI);

		assertThat(content).isEqualTo(VALID_CONTENT_JSON);
	}

	@Test
	@DisplayName("recommend와 solution이 없으면 빈 배열로 정규화한다")
	void analyzeNormalizesMissingListsToEmptyArrays() {
		givenModelResponds("{\"report\":[{\"subTitle\":\"영상에서 이런 행동이 보여요\",\"description\":\"산책 중이에요.\"}]}");

		String content = analyzer.analyze(S3_URI);

		assertThat(content)
				.contains("\"recommend\":[]")
				.contains("\"solution\":[]");
	}

	@Test
	@DisplayName("응답에서 JSON 객체를 찾지 못하면 분석에 실패한다")
	void analyzeFailsWhenResponseHasNoJsonObject() {
		givenModelResponds("분리불안 징후가 관찰됩니다.");

		assertThatThrownBy(() -> analyzer.analyze(S3_URI))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("응답이 JSON 형식이 아니면 분석에 실패한다")
	void analyzeFailsWhenResponseIsMalformedJson() {
		givenModelResponds("{\"recommend\": [잘못된 형식}");

		assertThatThrownBy(() -> analyzer.analyze(S3_URI))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("report 항목이 비어 있으면 분석에 실패한다")
	void analyzeFailsWhenReportIsEmpty() {
		givenModelResponds("{\"recommend\":[],\"report\":[],\"solution\":[]}");

		assertThatThrownBy(() -> analyzer.analyze(S3_URI))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("설정된 모델 ID로 요청한다")
	void analyzeUsesConfiguredModelId() {
		givenModelResponds(VALID_CONTENT_JSON);

		analyzer.analyze(S3_URI);

		assertThat(capturedRequest().modelId()).isEqualTo(MODEL_ID);
	}

	@Test
	@DisplayName("설정된 서비스 등급으로 요청한다")
	void analyzeUsesConfiguredServiceTier() {
		givenModelResponds(VALID_CONTENT_JSON);

		analyzer.analyze(S3_URI);

		assertThat(capturedRequest().serviceTier().type()).isEqualTo(ServiceTierType.FLEX);
	}

	@Test
	@DisplayName("설정된 최대 토큰·온도로 요청한다")
	void analyzeUsesConfiguredInferenceConfig() {
		givenModelResponds(VALID_CONTENT_JSON);

		analyzer.analyze(S3_URI);

		assertThat(capturedRequest().inferenceConfig().maxTokens()).isEqualTo(4096);
		assertThat(capturedRequest().inferenceConfig().temperature()).isEqualTo(0.2f);
	}

	@Test
	@DisplayName("비디오 블록을 텍스트보다 먼저 보낸다")
	void analyzeSendsVideoBeforeText() {
		givenModelResponds(VALID_CONTENT_JSON);

		analyzer.analyze(S3_URI);

		// 순서가 뒤집히면 Nova가 지시를 무시하고 영어 장면 묘사로 빠진다
		List<ContentBlock> contents = capturedRequest().messages().getFirst().content();
		assertThat(contents.getFirst().video()).isNotNull();
		assertThat(contents.get(1).text()).isNotNull();
	}

	@Test
	@DisplayName("s3 URI를 비디오 s3Location으로 실어 보낸다")
	void analyzeAttachesS3UriAsS3Location() {
		givenModelResponds(VALID_CONTENT_JSON);

		analyzer.analyze(S3_URI);

		// presigned URL을 보내면 Bedrock이 s3 위치로 해석해 거부한다
		ContentBlock video = capturedRequest().messages().getFirst().content().getFirst();
		assertThat(video.video().source().s3Location().uri()).isEqualTo(S3_URI);
	}

	@Test
	@DisplayName("mp4 영상은 mp4 형식으로 보낸다")
	void analyzeUsesMp4FormatForMp4() {
		givenModelResponds(VALID_CONTENT_JSON);

		analyzer.analyze(S3_URI);

		ContentBlock video = capturedRequest().messages().getFirst().content().getFirst();
		assertThat(video.video().format()).isEqualTo(VideoFormat.MP4);
	}

	@Test
	@DisplayName("mov 영상은 mov 형식으로 보낸다")
	void analyzeUsesMovFormatForMov() {
		givenModelResponds(VALID_CONTENT_JSON);

		analyzer.analyze("s3://test-video-bucket/videos/training/7/key.mov");

		ContentBlock video = capturedRequest().messages().getFirst().content().getFirst();
		assertThat(video.video().format()).isEqualTo(VideoFormat.MOV);
	}

	@Test
	@DisplayName("분석 지시를 system 메시지로 보낸다")
	void analyzeSendsAnalysisInstructionAsSystemMessage() {
		givenModelResponds(VALID_CONTENT_JSON);

		analyzer.analyze(S3_URI);

		assertThat(capturedRequest().system().getFirst().text()).contains("반려견");
	}

	@Test
	@DisplayName("사용자 프롬프트에 교육 목록을 치환해 보낸다")
	void analyzeInjectsTopicsIntoUserPrompt() {
		when(topicFinder.findAllOrdered()).thenReturn(List.of(
				new TopicSummary(1L, "배변", "편안한 배변 습관 만들기"),
				new TopicSummary(2L, "분리불안", "혼자서도 편안하게")));
		givenModelResponds(VALID_CONTENT_JSON);

		analyzer.analyze(S3_URI);

		String userText = capturedRequest().messages().getFirst().content().get(1).text();
		assertThat(userText)
				.contains("배변: 편안한 배변 습관 만들기")
				.contains("분리불안: 혼자서도 편안하게")
				.doesNotContain("{{topics}}");
	}

	@Test
	@DisplayName("모델 응답이 비어 있으면 분석에 실패한다")
	void analyzeFailsWhenResponseIsBlank() {
		givenModelResponds(" ");

		assertThatThrownBy(() -> analyzer.analyze(S3_URI))
				.isInstanceOf(IllegalStateException.class);
	}
}
