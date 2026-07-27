package com.daesabu.meongcoach.user.application.provided;

/**
 * 소셜 로그인 결과. needsOnboarding은 프로필 행 존재 여부로 판단하며,
 * 클라이언트가 로그인 직후 화면을 분기하는 데 쓴다.
 */
public record SocialLoginResult(AuthToken token, boolean needsOnboarding) {
}
