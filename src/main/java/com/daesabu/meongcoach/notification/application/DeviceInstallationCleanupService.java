package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class DeviceInstallationCleanupService {

	private final DeviceInstallationRepository installations;

	public void deleteInvalid(List<String> fids) {
		installations.deleteAllByFidIn(fids);
	}
}
