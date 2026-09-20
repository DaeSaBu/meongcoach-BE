package com.daesabu.meongcoach.user.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import com.daesabu.meongcoach.user.domain.exception.AlreadyOnboardedException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserStatus status;

	public static User registerUser() {
		User user = new User();

		user.role = UserRole.ONBOARDING_USER;
		user.status = UserStatus.ACTIVE;

		return user;
	}

	public void promoteToUser() {
		if (this.role == UserRole.USER) {
			throw new AlreadyOnboardedException();
		}
		this.role = UserRole.USER;
	}

	public boolean isOnboarding() {
		return this.role == UserRole.ONBOARDING_USER;
	}

	public void withdraw() {
		this.status = UserStatus.WITHDRAWN;
	}

	public boolean isWithdrawn() {
		return status == UserStatus.WITHDRAWN;
	}
}
