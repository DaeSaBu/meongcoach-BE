package com.daesabu.meongcoach.architecture;

import org.springframework.modulith.core.SpringBean;

enum ExternalSystem {

	KAKAO("Kakao", "Kakao", false),
	APPLE("Apple", "Apple", false),
	GOOGLE("Google", "Google", false),
	EVOLINK("EvoLink", "EvoLink", false),
	REVENUECAT("RevenueCat", "RevenueCat", false),
	CLOUDFLARE_R2("Cloudflare R2", "R2", false),
	AWS_S3("AWS S3", "S3", false),
	AWS_SQS("AWS SQS", "Sqs", true);

	private final String displayName;
	private final String classNameKeyword;
	private final boolean inbound;

	ExternalSystem(String displayName, String classNameKeyword, boolean inbound) {
		this.displayName = displayName;
		this.classNameKeyword = classNameKeyword;
		this.inbound = inbound;
	}

	boolean isConnectedBy(SpringBean bean) {
		return bean.getType().getSimpleName().contains(classNameKeyword);
	}

	String declaration() {
		return "cloud \"" + displayName + "\" as " + alias();
	}

	String relationship(String moduleAlias) {
		if (inbound) {
			return alias() + " --> " + moduleAlias + " : \"delivers\"";
		}
		return moduleAlias + " --> " + alias() + " : \"calls\"";
	}

	private String alias() {
		return "External." + name();
	}
}
