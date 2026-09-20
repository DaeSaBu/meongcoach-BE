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

	/**
	 * 회원 가입은 첫 소셜 로그인 또는 테스트 계정 시드에서만 일어난다.
	 * 반드시 자격증명(SocialAccount/LocalAccount) 생성과 같은 트랜잭션에서 호출해야 한다.
	 */
	public static User registerOnboardingUser() {
		User user = new User();
		user.role = UserRole.ONBOARDING_USER;
		user.status = UserStatus.ACTIVE;
		return user;
	}

	/**
	 * 온보딩 완료 시 호출한다. 프로필 생성과 같은 트랜잭션에서 불러야 role과 프로필이 함께 커밋된다.
	 * 온보딩 완료 여부의 원천은 role 하나이므로, 이미 USER인 회원의 재승격은 온보딩 중복으로 거부한다.
	 */
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
