# ADR-0005: API 문서는 Spring REST Docs로 작성한다

## 상태

Accepted

## 날짜

2026-05-08

## 맥락

UniT는 프론트엔드와 백엔드가 명확한 API 계약을 공유해야 한다. API 요청, 응답, 에러 코드, 공통 응답 구조가 문서와 실제 구현 사이에서 어긋나면 개발 비용이 커진다.

Swagger/OpenAPI는 빠르게 API를 확인하기 좋지만, 테스트로 검증되지 않은 문서가 노출될 수 있다. UniT는 API 계약의 신뢰성을 우선한다.

## 결정

API 문서는 Spring REST Docs를 사용해 작성한다.

## 이유

- 테스트를 통과한 요청과 응답만 문서화할 수 있다.
- MockMvc 테스트와 자연스럽게 연결된다.
- 공통 응답, 에러 응답, 필드 설명을 명시적으로 관리할 수 있다.
- Asciidoctor를 통해 읽기 좋은 HTML 문서를 생성할 수 있다.

## 결과

- 문서화 대상 API는 REST Docs 테스트를 작성한다.
- 테스트에서 생성된 snippet은 build/generated-snippets 하위에 생성된다.
- 최종 문서는 src/docs/asciidoc의 Asciidoc 파일에서 snippet을 include해 구성한다.
- build/generated-snippets와 build/docs는 빌드 산출물이므로 Git에 커밋하지 않는다.

## 대안

- Swagger/OpenAPI: API 탐색은 편하지만 테스트 기반 문서화가 아니므로 초기 기준 문서로 선택하지 않았다.
- 수동 문서 작성: 자유도는 높지만 실제 구현과 불일치할 위험이 커 선택하지 않았다.
