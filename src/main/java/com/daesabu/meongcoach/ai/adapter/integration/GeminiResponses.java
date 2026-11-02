package com.daesabu.meongcoach.ai.adapter.integration;

import com.google.genai.types.Candidate;
import com.google.genai.types.FinishReason;
import com.google.genai.types.GenerateContentResponse;
import java.util.List;

final class GeminiResponses {

	private GeminiResponses() {
	}

	static String extractText(GenerateContentResponse response) {
		List<Candidate> candidates = response.candidates().orElse(List.of());
		if (candidates.isEmpty()) {
			throw new GeminiResponseException("응답에 candidates가 없습니다");
		}
		FinishReason finishReason = candidates.getFirst().finishReason()
				.orElse(new FinishReason(FinishReason.Known.FINISH_REASON_UNSPECIFIED));
		if (finishReason.knownEnum() == FinishReason.Known.MAX_TOKENS) {
			throw new GeminiResponseException("응답이 max_output_tokens 상한에 걸려 잘렸습니다");
		}
		if (finishReason.knownEnum() != FinishReason.Known.STOP) {
			throw new GeminiResponseException("응답이 정상 종료되지 않았습니다(" + finishReason + ")");
		}
		String text = response.text();
		if (text == null || text.isBlank()) {
			throw new GeminiResponseException("응답 내용이 비어 있습니다");
		}
		return text;
	}
}
