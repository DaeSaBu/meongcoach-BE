package com.daesabu.meongcoach.loadtest.domain;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * INSERT 부하 측정용 기록. 생성 시각과 요청한 회원 ID만 가지며, user 모듈 소속이라 user_id는 연관관계 없이 ID로만 참조한다.
 */
@Getter
@Entity
@Table(name = "load_test_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoadTestRecord extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	public static LoadTestRecord record(Long userId) {
		LoadTestRecord record = new LoadTestRecord();
		record.userId = userId;
		return record;
	}
}
