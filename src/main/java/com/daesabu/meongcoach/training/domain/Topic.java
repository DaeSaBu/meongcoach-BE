package com.daesabu.meongcoach.training.domain;

import static jakarta.persistence.FetchType.LAZY;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "topics")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Topic extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String detail;

	@Column(nullable = false, length = 512)
	private String iconUrl;

	@Column(nullable = false)
	private int sortOrder;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "training_category_id", nullable = false)
	private TrainingCategory trainingCategory;

	@OneToMany(mappedBy = "topic")
	@OrderBy("sortOrder ASC, id ASC")
	private List<Curriculum> curriculums = new ArrayList<>();
}
