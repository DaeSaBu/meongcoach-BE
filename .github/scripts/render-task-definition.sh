#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 5 ]; then
	echo "사용법: ${0} <input> <output> <container> <image-uri> <spring-profile>" >&2
	exit 2
fi

INPUT=${1}
OUTPUT=${2}
CONTAINER=${3}
IMAGE_URI=${4}
SPRING_PROFILE=${5}

if [ "${SPRING_PROFILE}" != "dev" ] && [ "${SPRING_PROFILE}" != "prod" ]; then
	echo "spring-profile은 dev 또는 prod여야 합니다." >&2
	exit 2
fi

jq \
	--arg container "${CONTAINER}" \
	--arg image "${IMAGE_URI}" \
	--arg profile "${SPRING_PROFILE}" '
	if ([.containerDefinitions[] | select(.name == $container)] | length) != 1 then
		error("배포 대상 컨테이너는 정확히 하나여야 합니다.")
	else
		(.containerDefinitions[] | select(.name == $container)) as $target
		| (
			["DB_HOST", "DB_NAME", "DB_USERNAME", "DB_PASSWORD", "JWT_SECRET", "KAKAO_AUDIENCES"] -
			([(($target.environment // []) + ($target.secrets // []))[] | .name] | unique)
		) as $missing
		| if ($missing | length) != 0 then
			error("필수 애플리케이션 설정이 없습니다: \($missing | join(", "))")
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
					| .environment = (
						((.environment // []) | map(select(.name != "SPRING_PROFILES_ACTIVE"))) +
						[{"name": "SPRING_PROFILES_ACTIVE", "value": $profile}]
					)
				else
					.
				end
			)
		  end
	end
	' "${INPUT}" > "${OUTPUT}"
