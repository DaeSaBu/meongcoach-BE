package com.daesabu.meongcoach.loadtest.application.required;

import com.daesabu.meongcoach.loadtest.domain.LoadTestRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 부하 측정 기록 리포지토리. Spring Data JPA가 런타임에 구현한다.
 */
public interface LoadTestRecordRepository extends JpaRepository<LoadTestRecord, Long> {
}
