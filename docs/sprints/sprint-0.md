# Sprint 0 완료 기록

## 완료 항목

- Kotlin/Spring Boot 프로젝트 초기화
- Spring Modulith 기반 모듈 구조 정의
- Docker Compose MySQL 구성
- Flyway V1 migration 작성
- 공통 API 응답/에러 응답 구현
- 공통 에러 테스트 작성
- REST Docs 문서 구조 구성
- CI 품질 게이트 구성
- Actuator health/metrics 구성
- ADR 문서 작성

## 검증

- ./gradlew clean check asciidoctor
- /actuator/health
- /actuator/health/readiness
- /actuator/prometheus

## 다음 Sprint 후보

- Member 인증/회원 기반 구현
- 학교 이메일 인증
- JWT 발급/재발급
- 공통 Security 설정 구체화
