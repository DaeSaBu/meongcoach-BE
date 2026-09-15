package com.daesabu.meongcoach.loadtest.application.provided;

import com.daesabu.meongcoach.loadtest.domain.LoadTestRecord;

public interface LoadTestRecorder {

	/**
	 * 요청한 회원 ID로 기록 행 하나를 INSERT하고 저장된 기록을 반환한다. 호출할 때마다 행이 누적된다.
	 */
	LoadTestRecord record(Long userId);
}
