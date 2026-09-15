package com.daesabu.meongcoach.loadtest.adapter.webapi.dto;

import com.daesabu.meongcoach.loadtest.domain.LoadTestRecord;
import java.time.LocalDateTime;

public record LoadTestRecordResponse(Long id, Long userId, LocalDateTime createdAt) {

	public static LoadTestRecordResponse from(LoadTestRecord record) {
		return new LoadTestRecordResponse(record.getId(), record.getUserId(), record.getCreatedAt());
	}
}
