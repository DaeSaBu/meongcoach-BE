package com.daesabu.meongcoach.user.application.required;

import com.daesabu.meongcoach.user.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 온보딩 완료 여부는 별도 플래그 없이 프로필 행 존재 여부로 판단하므로 상속받은 existsById(userId)를 사용한다.
 * 탈퇴 시 개인정보 삭제도 상속받은 deleteById(userId)로 하며, 온보딩 미완료 회원은 행이 없어도 무시된다.
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}
