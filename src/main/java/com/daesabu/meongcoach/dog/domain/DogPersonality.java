package com.daesabu.meongcoach.dog.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 강아지-성격 연결 엔티티. ERD의 personality_id VARCHAR(30)은 오기로 판단해 BIGINT FK로 정정했다.
 */
@Getter
@Entity
@Table(
		name = "dog_personalities",
		uniqueConstraints = @UniqueConstraint(columnNames = {"dog_id", "personality_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DogPersonality extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "dog_id", nullable = false)
	private Dog dog;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "personality_id", nullable = false)
	private Personality personality;

	public static DogPersonality assign(Dog dog, Personality personality) {
		DogPersonality dogPersonality = new DogPersonality();
		dogPersonality.dog = dog;
		dogPersonality.personality = personality;
		return dogPersonality;
	}
}
