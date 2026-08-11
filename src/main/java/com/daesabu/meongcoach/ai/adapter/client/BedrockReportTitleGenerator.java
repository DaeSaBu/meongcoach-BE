package com.daesabu.meongcoach.ai.adapter.client;

import com.daesabu.meongcoach.ai.application.required.ReportTitleGenerator;
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
import software.amazon.awssdk.services.bedrockruntime.model.ServiceTier;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;

/**
 * AWS Bedrock Converse로 리포트 제목을 생성하는 어댑터. 영상을 다시 분석하지 않고 이미 만든 리포트 JSON
 * 텍스트만 넘겨 한 줄 제목을 받는다. 실패는 그대로 전파하고, 삼킬지는 호출부인 AiReportGenerateService가 정한다.
 */
@Component
public class BedrockReportTitleGenerator implements ReportTitleGenerator {

	private static final String SYSTEM_PROMPT_PATH = "prompts/report-title/system.md";
	private static final String USER_PROMPT_PATH = "prompts/report-title/user.md";
	private static final String REPORT_PLACEHOLDER = "{{report}}";
	// ai_reports.title 컬럼 길이와 맞춘 상한. 모델이 규칙을 어기고 길게 쓰면 잘라서 저장 실패를 막는다
	private static final int MAX_TITLE_LENGTH = 200;

	private final BedrockRuntimeClient bedrockRuntimeClient;
	private final String modelId;
	private final ServiceTier serviceTier;
	private final InferenceConfiguration inferenceConfig;
	private final String systemPrompt;
	private final String userPrompt;

	public BedrockReportTitleGenerator(BedrockRuntimeClient bedrockRuntimeClient, BedrockProperties properties) {
		this.bedrockRuntimeClient = bedrockRuntimeClient;
		this.modelId = properties.model();
		this.serviceTier = ServiceTier.builder().type(properties.serviceTier()).build();
		this.inferenceConfig = InferenceConfiguration.builder()
				.maxTokens(properties.maxTokens())
				.temperature(properties.temperature())
				.build();
		this.systemPrompt = PromptLoader.load(SYSTEM_PROMPT_PATH);
		this.userPrompt = PromptLoader.load(USER_PROMPT_PATH);
		if (!userPrompt.contains(REPORT_PLACEHOLDER)) {
			throw new IllegalStateException(
					"사용자 프롬프트에 " + REPORT_PLACEHOLDER + " 자리가 없습니다: " + USER_PROMPT_PATH);
		}
	}

	@Override
	public String generateTitle(String reportContentJson) {
		ConverseRequest request = buildConverseRequest(reportContentJson);

		ConverseResponse response = bedrockRuntimeClient.converse(request);

		String rawTitle = extractContent(response);
		String title = sanitize(rawTitle);
		if (title.isBlank()) {
			throw new IllegalStateException("리포트 제목 생성 결과가 비어 있습니다");
		}
		return title;
	}

	private ConverseRequest buildConverseRequest(String reportContentJson) {
		String prompt = userPrompt.replace(REPORT_PLACEHOLDER, reportContentJson);
		return ConverseRequest.builder()
				.modelId(modelId)
				.serviceTier(serviceTier)
				.inferenceConfig(inferenceConfig)
				.system(SystemContentBlock.fromText(systemPrompt))
				.messages(Message.builder()
						.role(ConversationRole.USER)
						.content(ContentBlock.fromText(prompt))
						.build())
				.build();
	}

	private static String extractContent(ConverseResponse response) {
		return response.output().message().content().stream()
				.map(ContentBlock::text)
				.filter(Objects::nonNull)
				.collect(Collectors.joining());
	}

	// 모델이 지시를 어기고 코드 펜스, 여러 줄, 감싼 따옴표를 붙이는 경우가 있어 제목 한 줄로 정제한다
	private static String sanitize(String rawTitle) {
		String firstLine = rawTitle.lines()
				.map(String::strip)
				.filter(line -> !line.isBlank())
				.filter(line -> !line.startsWith("```"))
				.findFirst()
				.orElse("");
		String title = stripSurroundingQuotes(firstLine);
		if (title.length() > MAX_TITLE_LENGTH) {
			return title.substring(0, MAX_TITLE_LENGTH);
		}
		return title;
	}

	private static String stripSurroundingQuotes(String value) {
		String result = value;
		while (result.length() >= 2 && isQuote(result.charAt(0)) && isQuote(result.charAt(result.length() - 1))) {
			result = result.substring(1, result.length() - 1).strip();
		}
		return result;
	}

	private static boolean isQuote(char letter) {
		return letter == '"' || letter == '\'' || letter == '“' || letter == '”' || letter == '‘' || letter == '’';
	}
}
