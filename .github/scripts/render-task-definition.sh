#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 4 ] || [ "$#" -gt 5 ]; then
	echo "사용법: $0 <input> <output> <container> <image-uri> [spring-profile]" >&2
	exit 2
fi

input=$1
output=$2
container=$3
image_uri=$4
spring_profile=${5:-}

jq \
	--arg container "$container" \
	--arg image "$image_uri" \
	--arg profile "$spring_profile" '
	if ([.containerDefinitions[] | select(.name == $container)] | length) != 1 then
		error("배포 대상 컨테이너는 정확히 하나여야 합니다.")
	else
		del(
			.taskDefinitionArn,
			.revision,
			.status,
			.requiresAttributes,
			.compatibilities,
			.registeredAt,
			.registeredBy
		)
		| .containerDefinitions |= map(
			if .name == $container then
				.image = $image
				| if $profile != "" then
					.environment = (
						((.environment // []) | map(select(.name != "SPRING_PROFILES_ACTIVE"))) +
						[{"name": "SPRING_PROFILES_ACTIVE", "value": $profile}]
					)
				  else
					.
				  end
			else
				.
			end
		)
	end
	' "$input" > "$output"
