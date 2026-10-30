package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceInstallationRepository extends JpaRepository<DeviceInstallation, Long> {

	Optional<DeviceInstallation> findByFid(String fid);

	List<DeviceInstallation> findAllByUserIdInOrderByUserIdAscIdAsc(Collection<Long> userIds);

	void deleteByUserIdAndFid(Long userId, String fid);

	void deleteAllByFidIn(Collection<String> fids);

	void deleteAllByUserId(Long userId);
}
