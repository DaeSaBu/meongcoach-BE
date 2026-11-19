package com.daesabu.meongcoach.analysis.application.provided;

import com.daesabu.meongcoach.analysis.application.provided.dto.AiAnalysisRegisterRequest;
import jakarta.validation.Valid;

/**
 * AI 영상 분석 건 생성 능력.
 */
public interface AiAnalysisRegister {

	/**
	 * 무료 체험 한도와 영상 길이를 검증한 뒤 분석할 강아지와 보호자가 남긴 맥락을 담은 분석 건을 UPLOADING으로 저장하고,
	 * 분석 건 ID와 영상 업로드 URL을 돌려준다. 강아지 ID가 없으면 사용자가 선택한 강아지로 저장한다.
	 * 한도를 소진했으면 {@code AiReportTrialExceededException}을,
	 * 길이가 10초 이상 300초 이하가 아니면 {@code AiVideoDurationOutOfRangeException}을,
	 * 지정한 강아지가 본인 소유가 아니거나 선택된 강아지가 없으면 {@code DogNotFoundException}을 던진다.
	 */
	AiAnalysisRegisterResult register(Long userId, Long dogId, @Valid AiAnalysisRegisterRequest request);
}
