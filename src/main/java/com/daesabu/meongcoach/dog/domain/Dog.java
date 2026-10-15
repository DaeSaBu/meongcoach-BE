package com.daesabu.meongcoach.dog.domain;

import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.Personality;
import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "dogs")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dog extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false, length = 50)
	private String name;

	@Embedded
	private Breed breed;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private DogSex sex;

	private LocalDate birthDate;

	@Column(nullable = false)
	private boolean birthDateEstimated;

	@Column(precision = 5, scale = 2)
	private BigDecimal weightKg;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DogStatus status;

	@Column(nullable = false, length = 512)
	private String profileImageUrl;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String expectation;

	private LocalDateTime deletedAt;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(
			name = "dog_personalities",
			joinColumns = @JoinColumn(name = "dog_id"),
			uniqueConstraints = @UniqueConstraint(columnNames = {"dog_id", "personality"}))
	@Enumerated(EnumType.STRING)
	@Column(name = "personality", nullable = false, length = 30)
	private Set<Personality> personalities = new HashSet<>();

	private Dog(DogRegisterCommand command) {
		this.userId = command.userId();
		this.name = command.name();
		this.breed = command.breed();
		this.sex = command.sex();
		this.birthDate = command.birthDate();
		this.birthDateEstimated = command.birthDateEstimated();
		this.weightKg = command.weightKg();
		this.status = DogStatus.UNSELECTED;
		this.profileImageUrl = Objects.requireNonNullElse(command.profileImageUrl(), "");
		this.expectation = Objects.requireNonNullElse(command.expectation(), "");
	}

	public static Dog register(DogRegisterCommand command) {
		return new Dog(command);
	}

	public boolean isSelected() {
		return status == DogStatus.SELECTED;
	}

	public void select() {
		this.status = DogStatus.SELECTED;
	}

	public void unselect() {
		this.status = DogStatus.UNSELECTED;
	}

	public void updateProfile(DogProfileUpdateCommand command) {
		this.name = command.name();
		this.breed = command.breed();
		this.sex = command.sex();
		this.birthDate = command.birthDate();
		this.birthDateEstimated = command.birthDateEstimated();
		this.weightKg = command.weightKg();
		this.profileImageUrl = Objects.requireNonNullElse(command.profileImageUrl(), "");
		this.expectation = Objects.requireNonNullElse(command.expectation(), "");
		changePersonalities(command.personalities());
	}

	public void changeExpectation(String expectation) {
		this.expectation = Objects.requireNonNullElse(expectation, "");
	}

	public void changePersonalities(Set<Personality> personalities) {
		this.personalities = new HashSet<>(personalities);
	}

	void delete() {
		this.deletedAt = LocalDateTime.now();
	}

	public Integer getAge() {
		if (birthDate == null) {
			return null;
		}
		return Period.between(birthDate, LocalDate.now()).getYears();
	}

	public Integer getAgeInMonths() {
		if (birthDate == null) {
			return null;
		}
		Period period = Period.between(birthDate, LocalDate.now());
		return period.getYears() * 12 + period.getMonths();
	}
}
