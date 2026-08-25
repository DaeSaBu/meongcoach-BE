package com.daesabu.meongcoach.dog.domain;

import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.DogSex;
import com.daesabu.meongcoach.dog.domain.shared.Personality;
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
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

// 소프트 딜리트: 삭제 건은 엔티티 조회(findById 포함)에서 자동 제외된다. 복구·관리 목적의 조회는 네이티브 쿼리가 필요하다
@Getter
@Entity
@Table(name = "dogs")
@SQLRestriction("deleted_at IS NULL")
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

	// 프로필 이미지를 등록하지 않아도 되므로 미설정은 빈 문자열로 저장한다
	@Column(nullable = false, length = 512)
	private String profileImageUrl;

	// 기대 사항을 입력하지 않아도 등록할 수 있으므로 미설정은 빈 문자열로 저장한다
	@Column(nullable = false, columnDefinition = "TEXT")
	private String expectation;

	// 소프트 딜리트 시각. null이면 삭제되지 않은 강아지다
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
		this.weightKg = command.weightKg();
		// 선택 여부는 다른 강아지가 있는지 봐야 정해지므로, 등록 시점에는 미선택으로 두고 Dogs가 select()로 정한다
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

	// 수정 화면이 전체 값을 다시 보내는 전체 교체라 필드별 메서드 대신 한 번에 바꾼다. 소유자·선택 상태는 건드리지 않는다
	public void updateProfile(DogProfileUpdateCommand command) {
		// 코드 변환을 먼저 끝내야 성격 코드가 잘못됐을 때 이름·견종만 바뀐 채 예외가 나가지 않는다
		Breed parsedBreed = Breed.from(command.breed());
		DogSex parsedSex = DogSex.from(command.sex());
		Set<Personality> parsedPersonalities = Personality.fromCodes(command.personalities());
		this.name = command.name();
		this.breed = parsedBreed;
		this.sex = parsedSex;
		this.birthDate = command.birthDate();
		this.weightKg = command.weightKg();
		this.profileImageUrl = Objects.requireNonNullElse(command.profileImageUrl(), "");
		this.expectation = Objects.requireNonNullElse(command.expectation(), "");
		changePersonalities(parsedPersonalities);
	}

	public void changePersonalities(Set<Personality> personalities) {
		this.personalities = new HashSet<>(personalities);
	}

	// 행을 지우지 않고 삭제 시각만 기록한다. 선택 상태는 건드리지 않는다 — 이후 조회에서 제외되므로 선택된 강아지가 없는 상태가 된다
	public void delete() {
		this.deletedAt = LocalDateTime.now();
	}

	// 나이는 저장하지 않고 생년월일로 계산한다
	public Integer getAge() {
		if (birthDate == null) {
			return null;
		}
		return Period.between(birthDate, LocalDate.now()).getYears();
	}
}
