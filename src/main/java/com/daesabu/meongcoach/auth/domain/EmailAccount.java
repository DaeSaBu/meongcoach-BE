package com.daesabu.meongcoach.auth.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "email_accounts",
		uniqueConstraints = @UniqueConstraint(columnNames = "email")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailAccount extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, unique = true)
	private Long userId;

	@Embedded
	private Email email;

	@Column(nullable = false, length = 255)
	private String passwordHash;

	public boolean isValidPassword(String password, PasswordMatcher matcher) {
		return matcher.matches(password, this.passwordHash);
	}
}
