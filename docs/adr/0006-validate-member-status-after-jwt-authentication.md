# ADR-0006: JWT 인증 후 회원 상태를 추가 검증한다

## 상태

Accepted

## 날짜

2026-05-14

## 맥락

UniT는 Access Token을 JWT로 발급한다. JWT는 stateless 방식이므로 서버가 토큰 원문을 저장하지 않고, 요청 시 서명과 만료 시간으로 유효성을 판단한다.

이 구조에서는 회원이 탈퇴하거나 정지되어도 이미 발급된 Access Token 자체는 만료 시각 전까지 서명 검증을 통과할 수 있다. Refresh Token을 폐기하더라도 기존 Access Token으로 인증 API에 접근할 수 있는 시간이 남는다.

탈퇴, 정지, 삭제 상태는 계정 접근 권한에 직접 영향을 주므로 JWT의 기술적 유효성과 별개로 현재 회원 상태를 확인해야 한다.

## 결정

Spring Security의 JWT 인증이 끝난 뒤, 인가 처리 전에 회원 상태 검증 필터를 실행한다.

필터는 인증 객체에서 JWT를 가져오고, 공개 인터페이스인 `JwtAuthenticationValidator` 목록을 실행한다. `member` 모듈은 이 인터페이스 구현체를 제공하여 JWT subject의 회원 ID가 `PENDING` 또는 `ACTIVE` 상태이고 `deleted_at`이 없는지 확인한다.

검증에 실패하면 요청을 컨트롤러로 전달하지 않고 `401 INVALID_TOKEN` 응답을 반환한다.

## 이유

- JWT Access Token은 stateless라 서버에서 특정 토큰을 직접 삭제할 수 없다.
- Redis 기반 blacklist는 MVP 단계에서 운영 부담이 크다.
- Access Token 만료 시간을 짧게 두는 것만으로는 탈퇴 또는 정지 직후 접근 차단을 보장하지 못한다.
- 회원 상태 검증은 탈퇴뿐 아니라 정지, 삭제, 향후 운영자 제재에도 공통으로 필요하다.
- `platform`은 검증 인터페이스만 알고, 실제 회원 조회는 `member` 구현체가 맡으면 모듈 경계를 유지할 수 있다.

## 결과

- 인증이 필요한 요청마다 회원 상태 조회가 1회 추가된다.
- 탈퇴, 정지, 삭제 회원의 기존 Access Token도 즉시 차단할 수 있다.
- 트래픽이 증가하면 캐시, token version, Redis blacklist 같은 방식으로 최적화할 수 있다.
- permit-all endpoint에는 회원 상태 검증이 적용되지 않도록 필터 순서와 인증 객체 존재 여부를 주의해야 한다.

## 대안

- Access Token 만료 시간을 매우 짧게 둔다: 구현은 단순하지만 즉시 차단을 보장하지 못한다.
- Redis blacklist를 사용한다: 즉시 차단은 가능하지만 MVP 단계에서는 인프라와 운영 복잡도가 증가한다.
- JWT에 token version을 넣고 DB 또는 캐시와 비교한다: 장기적으로 좋은 방식이지만 현재 단계에서는 상태 검증 필터보다 구현 범위가 크다.
