package com.daesabu.meongcoach.ai.adapter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;

/**
 * 모델 호출은 mock ChatModel로 가로채고, 프롬프트에 실리는 영상 미디어와 응답 처리를 검증한다.
 */
@DisplayName("Bedrock 영상 분석 어댑터")
class BedrockVideoAnalyzerTest {

	private static final String S3_URI = "s3://test-video-bucket/videos/training/7/key.mp4";

	private ChatModel chatModel;
	private BedrockVideoAnalyzer analyzer;

	@BeforeEach
	void setUp() {
		chatModel = mock(ChatModel.class);
		// ChatClient가 요청 조립 시 모델 기본 옵션을 병합하므로 빈 옵션을 돌려준다
		when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
		analyzer = new BedrockVideoAnalyzer(chatModel);
	}

	private void givenModelResponds(String content) {
		when(chatModel.call(any(Prompt.class)))
				.thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(content)))));
	}

	private UserMessage capturedUserMessage() {
		ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
		verify(chatModel).call(captor.capture());
		return captor.getValue().getInstructions().stream()
				.filter(UserMessage.class::isInstance)
				.map(UserMessage.class::cast)
				.findFirst()
				.orElseThrow();
	}

	@Test
	@DisplayName("모델 응답을 분석 결과로 반환한다")
	void analyzeReturnsModelResponse() {
		givenModelResponds("분리불안 징후가 관찰됩니다.");

		String content = analyzer.analyze(S3_URI);

		assertThat(content).isEqualTo("분리불안 징후가 관찰됩니다.");
	}

	@Test
	@DisplayName("s3 URI를 미디어로 실어 보낸다")
	void analyzeAttachesS3UriAsMedia() {
		givenModelResponds("결과");

		analyzer.analyze(S3_URI);

		// presigned URL을 보내면 Bedrock이 s3 위치로 해석해 거부한다
		assertThat(capturedUserMessage().getMedia().getFirst().getData()).isEqualTo(S3_URI);
	}

	@Test
	@DisplayName("mp4 영상은 video/mp4 형식으로 보낸다")
	void analyzeUsesMp4MimeTypeForMp4() {
		givenModelResponds("결과");

		analyzer.analyze(S3_URI);

		Media media = capturedUserMessage().getMedia().getFirst();
		assertThat(media.getMimeType().toString()).isEqualTo("video/mp4");
	}

	@Test
	@DisplayName("mov 영상은 video/quicktime 형식으로 보낸다")
	void analyzeUsesQuickTimeMimeTypeForMov() {
		givenModelResponds("결과");

		analyzer.analyze("s3://test-video-bucket/videos/training/7/key.mov");

		Media media = capturedUserMessage().getMedia().getFirst();
		assertThat(media.getMimeType().toString()).isEqualTo("video/quicktime");
	}

	@Test
	@DisplayName("분석 지시 프롬프트를 함께 보낸다")
	void analyzeSendsAnalysisPrompt() {
		givenModelResponds("결과");

		analyzer.analyze(S3_URI);

		assertThat(capturedUserMessage().getText()).contains("반려견");
	}

	@Test
	@DisplayName("모델 응답이 비어 있으면 분석에 실패한다")
	void analyzeFailsWhenResponseIsBlank() {
		givenModelResponds(" ");

		assertThatThrownBy(() -> analyzer.analyze(S3_URI))
				.isInstanceOf(IllegalStateException.class);
	}
}
