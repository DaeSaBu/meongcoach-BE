package com.daesabu.meongcoach.training.domain;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "lessons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lesson extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private Integer estimatedMinutes;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "curriculum_id", nullable = false)
	private Curriculum curriculum;

	@OneToMany(mappedBy = "lesson")
	@OrderBy("sortOrder ASC, id ASC")
	private List<Card> cards = new ArrayList<>();

	public Optional<EntitlementType> findRequiredEntitlementType() {
		return curriculum.findRequiredEntitlementType();
	}
}
