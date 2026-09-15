package com.daesabu.meongcoach.loadtest.adapter.webapi;

import com.daesabu.meongcoach.loadtest.adapter.webapi.dto.LoadTestRecordResponse;
import com.daesabu.meongcoach.loadtest.application.provided.LoadTestRecorder;
import com.daesabu.meongcoach.loadtest.domain.LoadTestRecord;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * INSERT 부하 측정용 기록 API. 검증·연관 조회 없이 행 하나만 INSERT해 순수 쓰기 성능을 잰다.
 */
@RestController
@RequestMapping("/api/loadtest/records")
@RequiredArgsConstructor
public class LoadTestRecordController {

	private final LoadTestRecorder loadTestRecorder;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public LoadTestRecordResponse record(@CurrentUserId Long userId) {
		LoadTestRecord record = loadTestRecorder.record(userId);
		return LoadTestRecordResponse.from(record);
	}
}
