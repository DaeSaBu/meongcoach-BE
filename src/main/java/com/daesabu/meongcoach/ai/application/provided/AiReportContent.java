package com.daesabu.meongcoach.ai.application.provided;

import java.util.List;

/**
 * AI 분석 리포트 본문 구조. 프론트가 컴포넌트별로 스타일을 입힐 수 있도록 recommend, report, solution으로 나눈다.
 * 문제 행동이 아닌 영상(반려견 없음, 정상 행동)은 recommend와 solution이 빈 배열이고, report는 항상 채워진다.
 */
public record AiReportContent(
		List<Recommend> recommend,
		List<ReportSection> report,
		List<Solution> solution) {

	public AiReportContent {
		recommend = emptyIfNull(recommend);
		report = emptyIfNull(report);
		solution = emptyIfNull(solution);
	}

	private static <T> List<T> emptyIfNull(List<T> values) {
		if (values == null) {
			return List.of();
		}
		return values;
	}

	/**
	 * 추천 교육. title은 교육 목록에 있는 교육 이름 그대로고, description은 이 교육을 추천하는 이유를
	 * 보호자에게 설명하는 문구다. description 도입 전에 저장된 리포트는 null일 수 있다.
	 */
	public record Recommend(String title, String description) {
	}

	/**
	 * 리포트 문단. subTitle 아래에 description을 보여준다.
	 */
	public record ReportSection(String subTitle, String description) {
	}

	/**
	 * 교정 단계. order는 1부터 차례대로 붙는다.
	 */
	public record Solution(Integer order, String title, String description) {
	}
}
