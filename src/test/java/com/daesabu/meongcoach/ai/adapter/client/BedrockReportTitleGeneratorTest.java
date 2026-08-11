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

/**
 * 모델 호출은 mock 클라이언트로 가로채고, 제목 생성 요청 구성과 응답 정제를 검증한다.
 */
@DisplayName("Bedrock 리포트 제목 생성 어댑터")
class BedrockReportTitleGeneratorTest {

	private static final String MODEL_ID = "test.nova-video-v1:0";
	private static final String REPORT_JSON = "{\"recommend\":[],\"report\":[{\"subTitle\":\"영상에서 이런 행동이 보여요\","
			+ "\"description\":\"물체를 물고 달려요.\"}],\"solution\":[]}";

	private BedrockRuntimeClient client;
	private BedrockReportTitleGenerator generator;

	@BeforeEach
	void setUp() {
		client = mock(BedrockRuntimeClient.class);
		generator = new BedrockReportTitleGenerator(client, properties());
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
	@DisplayName("모델이 반환한 제목을 그대로 반환한다")
	void generateTitleReturnsModelResponse() {
		givenModelResponds("물체를 물고 달리는 행동 분석");

		String title = generator.generateTitle(REPORT_JSON);

		assertThat(title).isEqualTo("물체를 물고 달리는 행동 분석");
	}

	@Test
	@DisplayName("설정된 모델 ID·서비스 등급·추론 설정으로 요청한다")
	void generateTitleUsesConfiguredModelSettings() {
		givenModelResponds("물체를 물고 달리는 행동 분석");

		generator.generateTitle(REPORT_JSON);

		ConverseRequest request = capturedRequest();
		assertThat(request.modelId()).isEqualTo(MODEL_ID);
		assertThat(request.serviceTier().type()).isEqualTo(ServiceTierType.FLEX);
		assertThat(request.inferenceConfig().maxTokens()).isEqualTo(4096);
		assertThat(request.inferenceConfig().temperature()).isEqualTo(0.2f);
	}

	@Test
	@DisplayName("영상 없이 텍스트 블록 하나만 보낸다")
	void generateTitleSendsSingleTextBlockWithoutVideo() {
		givenModelResponds("물체를 물고 달리는 행동 분석");

		generator.generateTitle(REPORT_JSON);

		List<ContentBlock> contents = capturedRequest().messages().getFirst().content();
		assertThat(contents).hasSize(1);
		assertThat(contents.getFirst().text()).isNotNull();
		assertThat(contents.getFirst().video()).isNull();
	}

	@Test
	@DisplayName("사용자 프롬프트에 리포트 JSON을 치환해 보낸다")
	void generateTitleInjectsReportIntoUserPrompt() {
		givenModelResponds("물체를 물고 달리는 행동 분석");

		generator.generateTitle(REPORT_JSON);

		String userText = capturedRequest().messages().getFirst().content().getFirst().text();
		assertThat(userText)
				.contains(REPORT_JSON)
				.doesNotContain("{{report}}");
	}

	@Test
	@DisplayName("제목 지시를 system 메시지로 보낸다")
	void generateTitleSendsInstructionAsSystemMessage() {
		givenModelResponds("물체를 물고 달리는 행동 분석");

		generator.generateTitle(REPORT_JSON);

		assertThat(capturedRequest().system().getFirst().text()).contains("제목");
	}

	@Test
	@DisplayName("코드 펜스와 여러 줄 응답에서 제목 한 줄만 추린다")
	void generateTitleKeepsFirstLineWithoutFences() {
		givenModelResponds("```\n물체를 물고 달리는 행동 분석\n부연 설명입니다.\n```");

		String title = generator.generateTitle(REPORT_JSON);

		assertThat(title).isEqualTo("물체를 물고 달리는 행동 분석");
	}

	@Test
	@DisplayName("제목을 감싼 따옴표를 제거한다")
	void generateTitleStripsSurroundingQuotes() {
		givenModelResponds("\"물체를 물고 달리는 행동 분석\"");

		String title = generator.generateTitle(REPORT_JSON);

		assertThat(title).isEqualTo("물체를 물고 달리는 행동 분석");
	}

	@Test
	@DisplayName("200자를 넘는 제목은 200자로 자른다")
	void generateTitleTruncatesTitleOverMaxLength() {
		givenModelResponds("가".repeat(250));

		String title = generator.generateTitle(REPORT_JSON);

		assertThat(title).hasSize(200);
	}

	@Test
	@DisplayName("모델 응답이 비어 있으면 제목 생성에 실패한다")
	void generateTitleFailsWhenResponseIsBlank() {
		givenModelResponds(" ");

		assertThatThrownBy(() -> generator.generateTitle(REPORT_JSON))
				.isInstanceOf(IllegalStateException.class);
	}
}
