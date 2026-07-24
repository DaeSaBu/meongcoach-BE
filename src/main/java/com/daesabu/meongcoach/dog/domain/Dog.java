package com.daesabu.meongcoach.dog.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
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

	@Column(length = 50)
	private String breed;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private DogSex sex;

	// 타임존 무관 — LocalDate 유지
	private LocalDate birthDate;

	@Column(precision = 5, scale = 2)
	private BigDecimal weightKg;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private DogSize size;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DogStatus status;

	@Column(length = 512)
	private String profileImageUrl;

	public static Dog register(Long userId, String name, String breed, DogSex sex, LocalDate birthDate,
	                           BigDecimal weightKg, DogSize size) {
		Dog dog = new Dog();
		dog.userId = userId;
		dog.name = name;
		dog.breed = breed;
		dog.sex = sex;
		dog.birthDate = birthDate;
		dog.weightKg = weightKg;
		dog.size = size;
		dog.status = DogStatus.SELECTED;
		return dog;
	}

	public void select() {
		this.status = DogStatus.SELECTED;
	}

	public void unselect() {
		this.status = DogStatus.UNSELECTED;
	}

	public void changeProfileImage(String profileImageUrl) {
		this.profileImageUrl = profileImageUrl;
	}

	// 나이는 저장하지 않고 생년월일로 계산한다
	public Integer getAge() {
		if (birthDate == null) {
			return null;
		}
		return Period.between(birthDate, LocalDate.now()).getYears();
	}
}
