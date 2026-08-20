---
name: git-convention
description: "이 저장소의 Git 컨벤션 — 변경 완료 시 즉시 커밋·push·draft PR 생성하는 작업 흐름, 커밋 메시지 type·scope·subject 규칙, 커밋 단위, 브랜치 규칙, push 규칙, PR·코드 리뷰 규칙, GitHub Actions 워크플로 규칙. Use when committing, creating branches, pushing, creating PRs, reviewing code, or after completing any change to the working tree. Triggers on: git commit, 커밋, 브랜치 생성, push, PR 생성, 코드 리뷰, 워크플로 수정, 코드 변경 완료."
user-invocable: true
---

# Git 컨벤션

이 저장소에서 커밋·브랜치·push·PR을 다룰 때 반드시 따르는 규칙이다.

---

## 작업 흐름

에이전트는 작업 브랜치에서 다음 루프를 따른다. 코드·문서·설정 구분 없이 모든 변경에 적용한다.

1. 하나의 의미 단위 작업이 끝나면 즉시 커밋한다. 여러 작업을 모아 통으로 커밋하지 않는다.
2. 커밋 후 곧바로 작업 브랜치에 push한다. 사용자의 push 요청을 기다리지 않는다.
3. 브랜치의 첫 커밋을 push했다면 즉시 draft PR을 생성한다. ([PR 컨벤션](#pr-컨벤션) 참고)
4. push할 때마다 커밋 메시지·변경 파일·PR 링크를 요약해 작업자에게 보고한다. 작업자가 코드 변경을 매 단계 인지할 수 있게 하기 위함이다.

---

## 커밋 메시지 컨벤션

모든 저장소와 자동화 도구가 다음 형식을 공통으로 쓴다.

```
<type>(<scope>): <subject>

[body]
```

- 프로젝트 저장소는 브랜치 흐름과 scope 후보만 추가로 정의할 수 있다.
- type 목록과 subject 문체는 프로젝트별로 다시 정의하지 않는다.

### Type

- 사용 가능한 type: `feature`(기능 추가), `fix`(동작 수정), `refactor`(동작 유지 구조 개선), `test`(테스트 추가·수정), `docs`(문서만 변경), `chore`(기능과
  무관한 설정·도구·유지보수), `style`(동작 변경 없는 서식·포맷팅).
- `perf`, `build`, `ci`, `init` 등 목록에 없는 type은 만들지 않는다.
- 성능 개선은 동작 변경 여부에 따라 `feature` 또는 `refactor`로 분류한다.
- 빌드·의존성·CI 설정은 `chore`를 쓴다.

### Scope

- 선택 사항. 프로젝트가 정의한 모듈·도메인 이름을 쓴다.
- 여러 모듈에 걸친 변경이나 적절한 이름이 없는 변경은 scope를 생략한다.
- 영문 소문자와 하이픈으로 작성한다.
- 이 저장소의 scope 후보는 Spring Modulith 모듈 패키지명과 같다. `user`(회원 계정·인증), `dog`(반려견 프로필), `training`(훈련 콘텐츠 카탈로그),
  `progress`(학습 진도), `ai`(AI 영상 분석), `onboarding`(온보딩 흐름 조합), `media`(이미지·영상 업로드), `health`(서비스 상태 확인), `shared`(횡단 관심사).
- 새 모듈을 추가하면 이 목록도 함께 갱신한다.

### Subject

- 실제 커밋에 포함된 작업 내용만 한글 명사형으로 간결하게 작성한다.
- `구현`, `추가`, `수정`, `정리`, `제거`, `구성`, `연결`처럼 끝낸다.
- `한다`/`했다`/`합니다` 같은 서술형 종결과 마침표를 쓰지 않는다.
- 파일명 나열, 작업 과정, 감정, 완료 여부는 적지 않는다.

### Body

- 변경 이유, 선택 근거, 호환성 영향을 설명해야 할 때만 작성한다.
- subject를 반복하거나 변경 파일을 나열하지 않는다.

### 커밋 단위

- 하나의 커밋은 하나의 목적만 가진다. 가능한 가장 작은 의미 단위로 나누고, 여러 의미 단위의 작업을 하나로 묶어 커밋하지 않는다.
- type이 다르거나 독립적으로 되돌릴 수 있는 변경은 분리한다.
- 서로 다른 기능·모듈·문서 주제의 변경을 한 커밋에 섞지 않는다.
- 기능과 직접 연관된 테스트는 기능 커밋에 포함할 수 있다. 테스트만 변경하면 `test`를 쓴다.
- WIP·fixup·임시 디버깅 커밋은 push 전에 정리한다.

### 작성 절차

1. `git status`와 staged diff로 포함 파일을 확인한다.
2. staged 변경의 단일 목적에 따라 type과 scope를 결정한다.
3. subject가 실제 변경 전체를 대표하는지 확인한다.
4. 목적이 다른 파일이 섞였으면 커밋을 분리한다.

---

## 브랜치 규칙

이 저장소가 추가로 정의하는 브랜치 흐름이다.

| 브랜치 | 용도 | 규칙 |
|---|---|---|
| `main` | 배포 기준 | 직접 push 금지, `develop`에서만 머지 |
| `develop` | 통합 개발 | 직접 push 금지, PR로만 머지 |
| `feature/{이슈번호}` | 기능 개발 | Linear 이슈 기준 생성, `develop`에서 분기 |

- 브랜치명에는 Linear가 제공하는 이슈번호를 소문자로 그대로 쓴다. (예: `feature/dae-179`)
- 작업 내용을 덧붙일 경우 `feature/dae-179-add-sns-login`처럼 영문 소문자와 하이픈으로 이어 쓴다.
- 한 이슈를 여러 브랜치로 나눠야 하는 경우 `feature/dae-179-2`처럼 뒤에 순번을 붙인다.
- 머지 방향은 `feature → develop → main`이다.
- 머지 전략은 아래 [PR 컨벤션](#pr-컨벤션)에서 정의한다.

---

## Push 규칙

- 작업 브랜치에서는 커밋을 만들면 즉시 push한다. ([작업 흐름](#작업-흐름) 참고)
- `main`·`develop`에는 직접 push하지 않는다.
- push 전에 대상 브랜치, 커밋 범위, 원격 상태를 확인한다.
- 일반 push에는 릴리스 노트, annotated tag, 버전 변경을 추가하지 않는다.
- `--force`는 금지한다.
- `--force-with-lease`는 원격 브랜치를 덮어쓰는 부작용을 인지한 후에만 사용한다.

---

## PR 컨벤션

- 브랜치에 첫 코드 수정 커밋이 만들어지면 즉시 해당 브랜치의 draft PR을 생성한다. 작업 방향을 일찍 공유해 리뷰 비용을 줄이기 위함이며, 리뷰 준비가 되면 draft를 해제한다.
- 제목은 커밋 메시지 규칙과 동일한 `<type>(<scope>): <subject>` 형식을 쓴다. (scope는 커밋과 동일하게 선택 사항)
- 설명에는 변경 사항 요약, 대상 Linear 이슈의 식별자·링크, 테스트 방법을 포함한다.
- 최소 1인 이상 승인 후 머지한다.
- 머지 전략은 저장소별로 통일한다 — Squash merge를 기본, 릴리스 브랜치는 Merge commit.

---

## 코드 리뷰 규칙

- 리뷰 승인 조건: CI 통과 + 최소 1인 승인 + 미해결 코멘트 없음, 모두 적용.
- 리뷰 코멘트에는 구체적인 개선 방향을 적는다.

---

## GitHub Actions 워크플로

- 외부 Action은 [GitHub 보안 권장사항](https://docs.github.com/en/actions/reference/security/secure-use)에 따라 full commit SHA로 고정하고, 사람이 버전을 알아볼 수 있도록 같은 줄의 주석에 release version을 기록한다.
