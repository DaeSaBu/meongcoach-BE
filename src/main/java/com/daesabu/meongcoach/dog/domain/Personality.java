package com.daesabu.meongcoach.dog.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;

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
 * 강아지 성격 마스터 데이터. 소심함 / 활발함 / 친화적 등.
 */
@Getter
@Entity
@Table(name = "personalities")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Personality extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 30)
	private String name;

	public static Personality create(String name) {
		Personality personality = new Personality();
		personality.name = name;
		return personality;
	}
}
