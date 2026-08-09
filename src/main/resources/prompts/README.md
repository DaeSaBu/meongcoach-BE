# 프롬프트 리소스

영상 분석 등 LLM 호출에 쓰는 프롬프트를 버전별로 관리한다. 여러 버전을 나란히 두고 비교 실험하기 위한 구조다.

## 디렉터리 규약

```
prompts/
└── video-analysis/          # 기능 단위
    ├── v1/                  # 프롬프트 버전
    │   ├── system.md        # system 메시지
    │   └── user.md          # user 메시지
    └── v2/
        ├── system.md
        └── user.md
```

## 버전 목록

- v1 — 최초 프롬프트. 마크다운 4항목 리포트 고정
- v2 — 평문(비마크다운) 출력, 소리·불확실 대상 환각 억제, 영상 유형(무관/문제 행동/일상·놀이) 분기.
  실호출 결과 Nova 2 Lite가 서식·제목·분기 지시를 무시해 실패 사례로 보존
- v3 — v2 개선판: 규칙 압축, 출력 예시(few-shot) 추가, user 프롬프트에 핵심 제약 반복

## 버전 전환 방법

`meongcoach.ai.bedrock.prompt-version` 프로퍼티가 사용할 버전 디렉터리를 가리킨다(기본 v1).

- `.env`에 `BEDROCK_PROMPT_VERSION=v2` 설정 후 재기동
- 실제 Bedrock 호출로 버전별 결과를 비교하려면 `BedrockVideoAnalyzerManualTest` 실행

## 주의

- 파일 내용은 모델에 **그대로** 전달된다. 파일 안에 HTML 주석 등 메타 주석을 넣지 않는다.
- 파일이 없거나 비어 있으면 애플리케이션 기동에 실패한다.
- 인코딩은 UTF-8로 저장한다.
