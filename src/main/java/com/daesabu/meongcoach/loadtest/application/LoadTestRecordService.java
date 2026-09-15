package com.daesabu.meongcoach.loadtest.application;

import com.daesabu.meongcoach.loadtest.application.provided.LoadTestRecorder;
import com.daesabu.meongcoach.loadtest.application.required.LoadTestRecordRepository;
import com.daesabu.meongcoach.loadtest.domain.LoadTestRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoadTestRecordService implements LoadTestRecorder {

	private final LoadTestRecordRepository loadTestRecordRepository;

	@Override
	@Transactional
	public LoadTestRecord record(Long userId) {
		LoadTestRecord record = LoadTestRecord.record(userId);
		return loadTestRecordRepository.save(record);
	}
}
