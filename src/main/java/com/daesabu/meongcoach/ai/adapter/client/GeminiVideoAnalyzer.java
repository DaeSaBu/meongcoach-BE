package com.daesabu.meongcoach.ai.adapter.client;

import com.daesabu.meongcoach.ai.application.required.VideoAnalyzer;
import java.net.URI;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

/**
 * Gemini로 영상을 분석하는 어댑터. 영상은 바이트로 내려받지 않고 presigned URL을 fileUri로 전달해
 * Gemini가 직접 가져가게 한다. 실패 시 예외를 전파하며, 호출자가 원인을 로그로 남기고 소비한다.
 */
@Component
public class GeminiVideoAnalyzer implements VideoAnalyzer {

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

	public GeminiVideoAnalyzer(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
	}

	@Override
	public String analyze(String videoUrl) {
		Media video = new Media(mimeTypeOf(videoUrl), URI.create(videoUrl));

		String content = chatClient.prompt()
				.user(user -> user.text(ANALYSIS_PROMPT).media(video))
				.call()
				.content();
		if (content == null || content.isBlank()) {
			throw new IllegalStateException("Gemini 영상 분석 결과가 비어 있습니다: " + videoUrl);
		}
		return content;
	}

	// presigned URL은 쿼리 파라미터가 붙어 있어 경로의 확장자로만 판단한다
	private static MimeType mimeTypeOf(String videoUrl) {
		String path = URI.create(videoUrl).getPath();
		if (path != null && path.endsWith(".mov")) {
			return VIDEO_QUICKTIME;
		}
		return VIDEO_MP4;
	}
}
