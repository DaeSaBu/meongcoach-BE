package com.daesabu.meongcoach.loadtest.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.loadtest.application.provided.LoadTestRecorder;
import com.daesabu.meongcoach.loadtest.domain.LoadTestRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * INSERT 부하 측정용 기록 서비스 검증.
 */
@DataJpaTest
@Import(LoadTestRecordService.class)
class LoadTestRecordServiceTest {

	private static final Long USER_ID = 1L;

	@Autowired
	private LoadTestRecorder loadTestRecorder;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 요청한_사용자_ID로_행을_저장한다() {
		LoadTestRecord record = loadTestRecorder.record(USER_ID);

		flushAndClear();
		LoadTestRecord saved = entityManager.find(LoadTestRecord.class, record.getId());
		assertThat(countRecords()).isOne();
		assertThat(saved.getUserId()).isEqualTo(USER_ID);
		assertThat(saved.getCreatedAt()).isNotNull();
	}

	@Test
	void 같은_사용자가_여러_번_호출하면_행이_누적된다() {
		loadTestRecorder.record(USER_ID);
		loadTestRecorder.record(USER_ID);
		loadTestRecorder.record(USER_ID);

		flushAndClear();
		assertThat(countRecords()).isEqualTo(3L);
	}

	private long countRecords() {
		return entityManager.getEntityManager()
				.createQuery("select count(r) from LoadTestRecord r", Long.class)
				.getSingleResult();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
