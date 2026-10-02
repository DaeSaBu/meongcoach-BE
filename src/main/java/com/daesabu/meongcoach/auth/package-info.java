/**
 * 인증 모듈. 소셜 로그인 검증·스토어 심사용 이메일 로그인과 JWT 발급을 담당하고, 소셜·로컬 계정(자격증명)을 관리한다.
 * 리프레시 토큰은 jti로 저장해 재발급 시 rotation하고 로그아웃·탈퇴 시 폐기한다.
 * 회원은 user 모듈의 provided API와 회원 ID로만 참조한다.
 */
@ApplicationModule
package com.daesabu.meongcoach.auth;

import org.springframework.modulith.ApplicationModule;
