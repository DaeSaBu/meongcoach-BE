package com.daesabu.meongcoach.user.application.provided;

/**
 * 로그인 결과. 소셜·이메일 로그인이 공통으로 반환한다. needsOnboarding은 프로필 행 존재 여부로 판단하며,
 * 클라이언트가 로그인 직후 화면을 분기하는 데 쓴다.
 */
public record LoginResult(AuthToken token, boolean needsOnboarding) {
}
