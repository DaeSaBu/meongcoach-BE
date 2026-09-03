/**
 * 회원 계정·인증 모듈. 계정, 프로필, 소셜 계정을 관리하고 소셜 로그인 검증·스토어 심사용 이메일 로그인과 JWT 발급을 담당한다.
 * 리프레시 토큰은 jti로 저장해 재발급 시 rotation하고 로그아웃·탈퇴 시 폐기한다.
 */
@ApplicationModule
package com.daesabu.meongcoach.user;

import org.springframework.modulith.ApplicationModule;
