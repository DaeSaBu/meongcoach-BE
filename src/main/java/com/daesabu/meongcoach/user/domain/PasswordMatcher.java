package com.daesabu.meongcoach.user.domain;

/**
 * 원문 비밀번호가 저장된 해시와 일치하는지 대조한다. 도메인이 해시 알고리즘(BCrypt)에 묶이지 않도록
 * 인터페이스만 두고, 구현은 user/adapter/security에서 스프링 PasswordEncoder를 감싸 제공한다.
 */
public interface PasswordMatcher {

	boolean matches(String rawPassword, String passwordHash);
}
