package com.daesabu.meongcoach.user.application.provided;

/**
 * 로컬 계정 생성 입력. 비밀번호는 평문으로 받고 해싱은 user 모듈이 수행한다.
 */
public record LocalAccountRegisterInfo(String email, String password) {
}
