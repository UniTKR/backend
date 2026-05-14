# ADR-0013: 로그인은 Spring Security 필터가 아니라 서비스 API로 구현한다

## 상태

Accepted

## 날짜

2026-05-14

## 맥락

UniT는 REST API 기반 백엔드이며 클라이언트는 자체 로그인, 소셜 로그인, Refresh Token 발급, 회원 상태 검증을 명확한 API 계약으로 다뤄야 한다.

Spring Security의 `UsernamePasswordAuthenticationFilter` 흐름을 사용하면 전통적인 form login 모델에는 잘 맞지만, API 응답 구조, Refresh Token 저장, 회원 상태별 에러 코드, 소셜 로그인 확장을 한 흐름에 맞추기 어렵다.

UniT는 공통 응답 구조와 에러 코드를 프론트 인터셉터에서 활용하기로 했으므로 로그인 실패와 성공 응답도 명시적인 API 계약으로 관리해야 한다.

## 결정

로그인은 `POST /api/v1/auth/login` 서비스 API로 구현한다.

컨트롤러는 요청 DTO를 검증하고, `AuthLoginService`가 이메일 해시 조회, 비밀번호 검증, 회원 상태 확인, Access Token 발급, Refresh Token 발급을 수행한다.

Spring Security는 로그인 처리보다 JWT Resource Server로서 보호된 API의 토큰 검증과 인가에 집중한다.

## 이유

- 공통 성공/실패 응답 구조를 로그인 API에도 일관되게 적용할 수 있다.
- Refresh Token 저장과 rotation 정책을 로그인 성공 흐름에 명시적으로 연결할 수 있다.
- 자체 로그인과 소셜 로그인을 같은 토큰 발급 정책으로 통합하기 쉽다.
- 회원 상태별 에러 코드와 메시지를 서비스 정책에 맞게 관리할 수 있다.
- REST Docs로 로그인 요청/응답 계약을 테스트 기반으로 문서화하기 쉽다.

## 결과

- Spring Security 기본 form login은 비활성화한다.
- 인증 성공 후 SecurityContext를 세션에 저장하지 않고 JWT 기반 stateless 인증을 사용한다.
- 로그인 서비스가 보안 핵심 로직을 포함하므로 테스트 범위를 충분히 확보해야 한다.
- 소셜 로그인 도입 시에도 최종 토큰 발급은 같은 응답 구조를 따르도록 설계한다.

## 대안

- Spring Security form login 필터 사용: 기본 기능은 많지만 REST API와 토큰 발급 정책을 맞추기 어렵다.
- OAuth2 Login 흐름만 사용: 자체 로그인 요구사항을 충족하지 못한다.
- 외부 인증 서비스로 위임: 장기적으로 가능하지만 현재는 자체 도메인 정책과 학교 인증 흐름을 직접 제어해야 한다.
