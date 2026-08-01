#!/usr/bin/env bash
set -euo pipefail

# 이 script는 ECS task definition JSON만 검증·변환해 stdout으로 출력한다.
# task definition 등록과 ECS service 갱신은 이 script를 호출한 CD workflow가 이어서 실행한다.

if [ "$#" -ne 3 ]; then
	echo "사용법: ${0} <input> <container> <image-uri>" >&2
	exit 2
fi

INPUT=${1}
CONTAINER=${2}
IMAGE_URI=${3}

# DB 외 애플리케이션 설정은 GitHub Secrets가 소유한다.
: "${JWT_SECRET:?JWT_SECRET이 필요합니다.}"
: "${KAKAO_AUDIENCES:?KAKAO_AUDIENCES가 필요합니다.}"
: "${R2_ENDPOINT:?R2_ENDPOINT가 필요합니다.}"
: "${R2_ACCESS_KEY_ID:?R2_ACCESS_KEY_ID가 필요합니다.}"
: "${R2_SECRET_ACCESS_KEY:?R2_SECRET_ACCESS_KEY가 필요합니다.}"
: "${R2_BUCKET:?R2_BUCKET이 필요합니다.}"
: "${R2_PUBLIC_BASE_URL:?R2_PUBLIC_BASE_URL이 필요합니다.}"

# jq 결과는 stdout으로 나간다. workflow가 이를 register-task-definition 입력 파일로 저장한다.
jq \
	--arg container "${CONTAINER}" \
	--arg image "${IMAGE_URI}" \
	--arg jwt_secret "${JWT_SECRET:-}" \
	--arg kakao_audiences "${KAKAO_AUDIENCES:-}" \
	--arg r2_endpoint "${R2_ENDPOINT:-}" \
	--arg r2_access_key_id "${R2_ACCESS_KEY_ID:-}" \
	--arg r2_secret_access_key "${R2_SECRET_ACCESS_KEY:-}" \
	--arg r2_bucket "${R2_BUCKET:-}" \
	--arg r2_public_base_url "${R2_PUBLIC_BASE_URL:-}" \
	--arg vimeo_access_token "${VIMEO_ACCESS_TOKEN:-}" '
	if ([.containerDefinitions[] | select(.name == $container)] | length) != 1 then
		error("배포 대상 컨테이너는 정확히 하나여야 합니다.")
	else
		(.containerDefinitions[] | select(.name == $container)) as $target
		| (
			[
				"DB_HOST",
				"DB_NAME",
				"DB_USERNAME",
				"DB_PASSWORD",
				"SPRING_PROFILES_ACTIVE"
				# "JWT_SECRET",
				# "KAKAO_AUDIENCES"
			] -
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
					.secrets = (
						(.secrets // []) |
						map(select(
							.name != "R2_ACCESS_KEY_ID" and
							.name != "R2_SECRET_ACCESS_KEY" and
							.name != "VIMEO_ACCESS_TOKEN"
						))
					)
					| .environment = (
						((.environment // []) |
							map(select(
								.name != "JWT_SECRET" and
								.name != "KAKAO_AUDIENCES" and
								.name != "R2_ENDPOINT" and
								.name != "R2_ACCESS_KEY_ID" and
								.name != "R2_SECRET_ACCESS_KEY" and
								.name != "R2_BUCKET" and
								.name != "R2_PUBLIC_BASE_URL" and
								.name != "VIMEO_ACCESS_TOKEN"
							))) +
						[
							{"name": "JWT_SECRET", "value": $jwt_secret},
							{"name": "KAKAO_AUDIENCES", "value": $kakao_audiences},
							{"name": "R2_ENDPOINT", "value": $r2_endpoint},
							{"name": "R2_ACCESS_KEY_ID", "value": $r2_access_key_id},
							{"name": "R2_SECRET_ACCESS_KEY", "value": $r2_secret_access_key},
							{"name": "R2_BUCKET", "value": $r2_bucket},
							{"name": "R2_PUBLIC_BASE_URL", "value": $r2_public_base_url}
						] +
						(if $vimeo_access_token == "" then
							[]
						 else
							[{"name": "VIMEO_ACCESS_TOKEN", "value": $vimeo_access_token}]
						 end)
					)
					| .image = $image
				else
					.
				end
			)
		  end
	end
	' "${INPUT}"
