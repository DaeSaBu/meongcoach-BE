package com.daesabu.meongcoach.dog.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.time.Period;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "dogs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dog extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// user 모듈 소속이라 ID로만 참조한다
	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false, length = 50)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Breed breed;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private DogSex sex;

	// 나이 미상 강아지를 허용하므로 nullable — getAge()가 null 반환. 타임존 무관 — LocalDate 유지
	private LocalDate birthDate;

	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal weightKg;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DogStatus status;

	// 등록 후 별도 API로 채워지는 값 — 미설정은 빈 문자열로 저장한다
	@Column(nullable = false, length = 512)
	private String profileImageUrl = "";

	// 온보딩에서 기대 사항을 입력하지 않아도 등록할 수 있으므로 미설정은 빈 문자열로 저장한다
	@Column(nullable = false, length = 500)
	private String expectation = "";

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
		this.weightKg = command.weightKg();
		this.status = DogStatus.SELECTED;
	}

	public static Dog register(DogRegisterCommand command) {
		return new Dog(command);
	}

	public void select() {
		this.status = DogStatus.SELECTED;
	}

	public void unselect() {
		this.status = DogStatus.UNSELECTED;
	}

	public void changeProfileImage(String profileImageUrl) {
		this.profileImageUrl = Objects.requireNonNullElse(profileImageUrl, "");
	}

	public void changeExpectation(String expectation) {
		this.expectation = Objects.requireNonNullElse(expectation, "");
	}

	public void changePersonalities(Set<Personality> personalities) {
		this.personalities = new HashSet<>(personalities);
	}

	// 나이는 저장하지 않고 생년월일로 계산한다
	public Integer getAge() {
		if (birthDate == null) {
			return null;
		}
		return Period.between(birthDate, LocalDate.now()).getYears();
	}
}
