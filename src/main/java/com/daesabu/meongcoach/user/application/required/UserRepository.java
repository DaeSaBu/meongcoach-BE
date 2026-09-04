package com.daesabu.meongcoach.user.application.required;

import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserRole;
import com.daesabu.meongcoach.user.domain.UserStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	// 인증 경로에서 요청마다 호출되므로 엔티티를 읽지 않고 역할만 조회한다. 탈퇴 회원은 행이 남아 있어 상태로 걸러낸다
	@Query("select u.role from User u where u.id = :userId and u.status <> :excludedStatus")
	Optional<UserRole> findRoleByIdAndStatusNot(@Param("userId") Long userId,
	                                            @Param("excludedStatus") UserStatus excludedStatus);

	boolean existsByIdAndStatusNot(Long userId, UserStatus excludedStatus);
}
