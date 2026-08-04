#!/usr/bin/env bash
# REST Docs HTML과 OpenAPI 스펙, Swagger UI 정적 파일을 GitHub Pages용 사이트 한 벌로 조립한다.
# 저장소 루트에서 실행하며, ./gradlew openapi3 asciidoctor 산출물이 있어야 한다.
# 사용법: SWAGGER_UI_VERSION=<버전> assemble-docs-site.sh <출력_디렉터리>
set -euo pipefail

OUTPUT_DIR="${1:?출력 디렉터리를 지정하세요}"
SWAGGER_UI_VERSION="${SWAGGER_UI_VERSION:?SWAGGER_UI_VERSION 환경 변수를 지정하세요}"

# 배포 사이트의 Try it out 대상 서버. 로컬 스펙(build.gradle.kts)은 localhost를 유지한다
DEV_API_URL="https://api.dev.meongcoach.com"

RESTDOCS_HTML_DIR="build/docs/asciidoc"
OPENAPI_SPEC="build/api-spec/openapi3.json"
test -f "${RESTDOCS_HTML_DIR}/index.html" || { echo "REST Docs HTML이 없습니다. ./gradlew asciidoctor 를 먼저 실행하세요." >&2; exit 1; }
test -f "${OPENAPI_SPEC}" || { echo "openapi3.json이 없습니다. ./gradlew openapi3 를 먼저 실행하세요." >&2; exit 1; }

rm -rf "${OUTPUT_DIR}"
mkdir -p "${OUTPUT_DIR}"

cp src/docs/site/index.html "${OUTPUT_DIR}/index.html"
cp -R "${RESTDOCS_HTML_DIR}" "${OUTPUT_DIR}/restdocs"

# Swagger UI는 릴리스 태그의 dist 정적 파일을 그대로 쓴다
DOWNLOAD_DIR=$(mktemp -d)
trap 'rm -rf "${DOWNLOAD_DIR}"' EXIT
curl --silent --show-error --fail --location \
  "https://github.com/swagger-api/swagger-ui/archive/refs/tags/v${SWAGGER_UI_VERSION}.tar.gz" \
  | tar -xz -C "${DOWNLOAD_DIR}"
cp -R "${DOWNLOAD_DIR}/swagger-ui-${SWAGGER_UI_VERSION}/dist" "${OUTPUT_DIR}/swagger-ui"

# 배포본 스펙은 로컬 서버가 아니라 dev API를 가리키도록 servers를 교체한다
jq --arg url "${DEV_API_URL}" '.servers = [{"url": $url}]' \
  "${OPENAPI_SPEC}" > "${OUTPUT_DIR}/swagger-ui/openapi3.json"

# 기본 initializer는 petstore 예시 스펙을 가리키므로 저장소 스펙을 로드하도록 교체한다
cat > "${OUTPUT_DIR}/swagger-ui/swagger-initializer.js" <<'EOF'
window.onload = function() {
  window.ui = SwaggerUIBundle({
    url: "./openapi3.json",
    dom_id: "#swagger-ui",
    deepLinking: true,
    presets: [SwaggerUIBundle.presets.apis],
    layout: "BaseLayout"
  });
};
EOF

echo "문서 사이트 조립 완료: ${OUTPUT_DIR}"
