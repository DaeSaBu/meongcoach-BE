package com.daesabu.meongcoach.training.domain;

import static jakarta.persistence.EnumType.STRING;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "training_categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingCategory extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false, length = 512)
	private String iconUrl;

	@Column(nullable = false)
	private int sortOrder;

	@Enumerated(STRING)
	@Column(length = 50)
	private EntitlementType requiredEntitlementType;

	@OneToMany(mappedBy = "trainingCategory")
	@OrderBy("sortOrder ASC, id ASC")
	private List<Topic> topics = new ArrayList<>();
}
