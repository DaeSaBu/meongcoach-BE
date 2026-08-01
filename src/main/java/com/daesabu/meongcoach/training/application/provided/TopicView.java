package com.daesabu.meongcoach.training.application.provided;

/**
 * 토픽 조회 결과.
 */
public record TopicView(
		Long id,
		String title,
		String description,
		String detail,
		String iconUrl,
		int sortOrder
) {
}
