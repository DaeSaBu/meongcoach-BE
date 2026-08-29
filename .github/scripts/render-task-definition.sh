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
: "${KAKAO_NATIVE_APP_KEY:?KAKAO_NATIVE_APP_KEY가 필요합니다.}"
: "${KAKAO_REST_API_KEY:?KAKAO_REST_API_KEY가 필요합니다.}"
: "${APPLE_BUNDLE_ID:?APPLE_BUNDLE_ID가 필요합니다.}"
: "${R2_ENDPOINT:?R2_ENDPOINT가 필요합니다.}"
: "${R2_ACCESS_KEY_ID:?R2_ACCESS_KEY_ID가 필요합니다.}"
: "${R2_SECRET_ACCESS_KEY:?R2_SECRET_ACCESS_KEY가 필요합니다.}"
: "${R2_BUCKET:?R2_BUCKET이 필요합니다.}"
: "${R2_PUBLIC_BASE_URL:?R2_PUBLIC_BASE_URL이 필요합니다.}"
: "${S3_REGION:?S3_REGION이 필요합니다.}"
: "${S3_ACCESS_KEY_ID:?S3_ACCESS_KEY_ID가 필요합니다.}"
: "${S3_SECRET_ACCESS_KEY:?S3_SECRET_ACCESS_KEY가 필요합니다.}"
: "${S3_BUCKET:?S3_BUCKET이 필요합니다.}"
: "${S3_PUBLIC_BASE_URL:?S3_PUBLIC_BASE_URL이 필요합니다.}"
# EVOLINK_BASE_URL·EVOLINK_MODEL은 application.yml에 기본값이 있어 선택 사항이다.
: "${EVOLINK_API_KEY:?EVOLINK_API_KEY가 필요합니다.}"
: "${SQS_REGION:?SQS_REGION이 필요합니다.}"
: "${SQS_ACCESS_KEY_ID:?SQS_ACCESS_KEY_ID가 필요합니다.}"
: "${SQS_SECRET_ACCESS_KEY:?SQS_SECRET_ACCESS_KEY가 필요합니다.}"
: "${AI_VIDEO_QUEUE:?AI_VIDEO_QUEUE가 필요합니다.}"

# jq 결과는 stdout으로 나간다. workflow가 이를 register-task-definition 입력 파일로 저장한다.
jq \
	--arg container "${CONTAINER}" \
	--arg image "${IMAGE_URI}" \
	--arg jwt_secret "${JWT_SECRET:-}" \
	--arg kakao_native_app_key "${KAKAO_NATIVE_APP_KEY:-}" \
	--arg kakao_rest_api_key "${KAKAO_REST_API_KEY:-}" \
	--arg apple_bundle_id "${APPLE_BUNDLE_ID:-}" \
	--arg r2_endpoint "${R2_ENDPOINT:-}" \
	--arg r2_access_key_id "${R2_ACCESS_KEY_ID:-}" \
	--arg r2_secret_access_key "${R2_SECRET_ACCESS_KEY:-}" \
	--arg r2_bucket "${R2_BUCKET:-}" \
	--arg r2_public_base_url "${R2_PUBLIC_BASE_URL:-}" \
	--arg s3_region "${S3_REGION:-}" \
	--arg s3_access_key_id "${S3_ACCESS_KEY_ID:-}" \
	--arg s3_secret_access_key "${S3_SECRET_ACCESS_KEY:-}" \
	--arg s3_bucket "${S3_BUCKET:-}" \
	--arg s3_public_base_url "${S3_PUBLIC_BASE_URL:-}" \
	--arg vimeo_access_token "${VIMEO_ACCESS_TOKEN:-}" \
	--arg evolink_api_key "${EVOLINK_API_KEY:-}" \
	--arg evolink_base_url "${EVOLINK_BASE_URL:-}" \
	--arg evolink_model "${EVOLINK_MODEL:-}" \
	--arg sqs_region "${SQS_REGION:-}" \
	--arg sqs_access_key_id "${SQS_ACCESS_KEY_ID:-}" \
	--arg sqs_secret_access_key "${SQS_SECRET_ACCESS_KEY:-}" \
	--arg ai_video_queue "${AI_VIDEO_QUEUE:-}" '
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
				# "KAKAO_NATIVE_APP_KEY",
				# "KAKAO_REST_API_KEY"
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
							.name != "S3_ACCESS_KEY_ID" and
							.name != "S3_SECRET_ACCESS_KEY" and
							.name != "VIMEO_ACCESS_TOKEN" and
							# GEMINI_API_KEY는 더 이상 주입하지 않는다. 이미 배포된 태스크 정의에서 걷어내려고 필터만 남긴다
							.name != "GEMINI_API_KEY" and
							.name != "SQS_ACCESS_KEY_ID" and
							.name != "SQS_SECRET_ACCESS_KEY" and
							# BEDROCK_*은 더 이상 주입하지 않는다. 이미 배포된 태스크 정의에서 걷어내려고 필터만 남긴다
							.name != "BEDROCK_ACCESS_KEY_ID" and
							.name != "BEDROCK_SECRET_ACCESS_KEY" and
							.name != "EVOLINK_API_KEY"
						))
					)
					| .environment = (
						((.environment // []) |
							map(select(
								.name != "JWT_SECRET" and
								.name != "KAKAO_AUDIENCES" and
								.name != "KAKAO_NATIVE_APP_KEY" and
								.name != "KAKAO_REST_API_KEY" and
								.name != "APPLE_BUNDLE_ID" and
								.name != "R2_ENDPOINT" and
								.name != "R2_ACCESS_KEY_ID" and
								.name != "R2_SECRET_ACCESS_KEY" and
								.name != "R2_BUCKET" and
								.name != "R2_PUBLIC_BASE_URL" and
								.name != "S3_REGION" and
								.name != "S3_ACCESS_KEY_ID" and
								.name != "S3_SECRET_ACCESS_KEY" and
								.name != "S3_BUCKET" and
								.name != "S3_PUBLIC_BASE_URL" and
								.name != "VIMEO_ACCESS_TOKEN" and
								# GEMINI_API_KEY는 더 이상 주입하지 않는다. 이미 배포된 태스크 정의에서 걷어내려고 필터만 남긴다
								.name != "GEMINI_API_KEY" and
								.name != "SQS_REGION" and
								.name != "SQS_ACCESS_KEY_ID" and
								.name != "SQS_SECRET_ACCESS_KEY" and
								.name != "AI_VIDEO_QUEUE" and
								# BEDROCK_*은 더 이상 주입하지 않는다. 이미 배포된 태스크 정의에서 걷어내려고 필터만 남긴다
								.name != "BEDROCK_REGION" and
								.name != "BEDROCK_ACCESS_KEY_ID" and
								.name != "BEDROCK_SECRET_ACCESS_KEY" and
								.name != "BEDROCK_MODEL" and
								.name != "EVOLINK_API_KEY" and
								.name != "EVOLINK_BASE_URL" and
								.name != "EVOLINK_MODEL"
							))) +
						[
							{"name": "JWT_SECRET", "value": $jwt_secret},
							{"name": "KAKAO_NATIVE_APP_KEY", "value": $kakao_native_app_key},
							{"name": "KAKAO_REST_API_KEY", "value": $kakao_rest_api_key},
							{"name": "APPLE_BUNDLE_ID", "value": $apple_bundle_id},
							{"name": "R2_ENDPOINT", "value": $r2_endpoint},
							{"name": "R2_ACCESS_KEY_ID", "value": $r2_access_key_id},
							{"name": "R2_SECRET_ACCESS_KEY", "value": $r2_secret_access_key},
							{"name": "R2_BUCKET", "value": $r2_bucket},
							{"name": "R2_PUBLIC_BASE_URL", "value": $r2_public_base_url},
							{"name": "S3_REGION", "value": $s3_region},
							{"name": "S3_ACCESS_KEY_ID", "value": $s3_access_key_id},
							{"name": "S3_SECRET_ACCESS_KEY", "value": $s3_secret_access_key},
							{"name": "S3_BUCKET", "value": $s3_bucket},
							{"name": "S3_PUBLIC_BASE_URL", "value": $s3_public_base_url},
							{"name": "SQS_REGION", "value": $sqs_region},
							{"name": "SQS_ACCESS_KEY_ID", "value": $sqs_access_key_id},
							{"name": "SQS_SECRET_ACCESS_KEY", "value": $sqs_secret_access_key},
							{"name": "AI_VIDEO_QUEUE", "value": $ai_video_queue},
							{"name": "EVOLINK_API_KEY", "value": $evolink_api_key}
						] +
						(if $vimeo_access_token == "" then
							[]
						 else
							[{"name": "VIMEO_ACCESS_TOKEN", "value": $vimeo_access_token}]
						 end) +
						# 미설정 시 application.yml의 기본값을 쓰도록 주입 자체를 생략한다
						(if $evolink_base_url == "" then
							[]
						 else
							[{"name": "EVOLINK_BASE_URL", "value": $evolink_base_url}]
						 end) +
						(if $evolink_model == "" then
							[]
						 else
							[{"name": "EVOLINK_MODEL", "value": $evolink_model}]
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
