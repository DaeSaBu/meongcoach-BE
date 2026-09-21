package com.daesabu.meongcoach.auth.application.required;

import com.daesabu.meongcoach.auth.domain.SocialProfile;
import com.daesabu.meongcoach.auth.domain.SocialProvider;

public interface SocialProfileReader {

	SocialProvider provider();

	SocialProfile read(String credential);
}
