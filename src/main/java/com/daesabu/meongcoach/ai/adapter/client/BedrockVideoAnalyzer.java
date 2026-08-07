package com.daesabu.meongcoach.ai.adapter.client;

import com.daesabu.meongcoach.ai.application.required.VideoAnalyzer;
import java.net.URI;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

/**
 * AWS Bedrock Converse로 영상을 분석하는 어댑터. 영상은 바이트로 내려받지 않고 s3://버킷/키 URI를 넘겨
 * Bedrock이 버킷에서 직접 읽게 하며, 그래서 호출 자격 증명에 영상 버킷의 s3:GetObject 권한이 필요하다.
 * 실패는 그대로 전파하고, 삼킬지는 호출부인 AiReportGenerateService가 정한다.
 */
@Component
public class BedrockVideoAnalyzer implements VideoAnalyzer {

	private static final MimeType VIDEO_MP4 = MimeType.valueOf("video/mp4");
	private static final MimeType VIDEO_QUICKTIME = MimeType.valueOf("video/quicktime");

	private static final String ANALYSIS_PROMPT = """
			당신은 반려견 행동 전문가입니다. 첨부된 반려견 영상을 분석해 보호자에게 전달할 행동 분석 리포트를 작성하세요.

			다음 내용을 담아 한국어로 작성합니다.
			1. 영상 속 반려견의 주요 행동 관찰 내용
			2. 행동에서 읽을 수 있는 감정 상태와 신호
			3. 주의가 필요한 문제 행동 징후 (없으면 없다고 명시)
			4. 보호자가 실천할 수 있는 훈련·개선 제안

			보호자가 이해하기 쉬운 표현을 쓰고, 영상에서 확인되지 않는 내용은 추측하지 마세요.
			""";

	private final ChatClient chatClient;

	// ChatClient 자동 설정을 쓰지 않으므로 모델을 받아 직접 감싼다
	public BedrockVideoAnalyzer(ChatModel chatModel) {
		this.chatClient = ChatClient.create(chatModel);
	}

	@Override
	public String analyze(String videoS3Uri) {
		Media video = new Media(mimeTypeOf(videoS3Uri), URI.create(videoS3Uri));

		String content = chatClient.prompt()
				.user(user -> user.text(ANALYSIS_PROMPT).media(video))
				.call()
				.content();
		if (content == null || content.isBlank()) {
			throw new IllegalStateException("영상 분석 결과가 비어 있습니다: " + videoS3Uri);
		}
		return content;
	}

	private static MimeType mimeTypeOf(String videoUri) {
		String path = URI.create(videoUri).getPath();
		if (path != null && path.endsWith(".mov")) {
			return VIDEO_QUICKTIME;
		}
		return VIDEO_MP4;
	}
}
