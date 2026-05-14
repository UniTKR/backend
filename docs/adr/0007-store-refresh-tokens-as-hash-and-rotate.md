# ADR-0007: Refresh Token은 해시로 저장하고 rotation한다

## 상태

Accepted

## 날짜

2026-05-14

## 맥락

UniT는 Access Token과 Refresh Token을 함께 발급한다. Access Token은 짧은 인증용 토큰이고, Refresh Token은 앱 로그인 유지를 위해 더 긴 기간 사용된다.

Refresh Token은 탈취되면 새로운 Access Token을 계속 발급받을 수 있으므로 민감도가 높다. DB에 원문을 저장하면 DB 유출 시 토큰이 그대로 재사용될 수 있다.

또한 같은 Refresh Token을 반복 사용하는 방식은 탈취 탐지와 세션 폐기가 어렵다.

## 결정

Refresh Token 원문은 클라이언트에만 전달하고 서버에는 SHA-256 해시를 저장한다.

토큰 갱신 시에는 기존 Refresh Token을 `ROTATED` 상태로 변경하고, 새로운 Refresh Token을 발급하여 `ACTIVE` 상태로 저장한다. 로그아웃 또는 회원 탈퇴 시에는 활성 Refresh Token을 `REVOKED` 상태로 변경한다. 만료된 토큰이 감지되면 `EXPIRED` 상태로 변경한다.

## 이유

- DB에는 Refresh Token 원문을 저장하지 않아 유출 피해를 줄인다.
- rotation을 사용하면 이미 사용된 토큰의 재사용을 차단할 수 있다.
- `ACTIVE`, `ROTATED`, `REVOKED`, `EXPIRED` 상태로 토큰 생명주기를 명확히 추적할 수 있다.
- 로그아웃과 회원 탈퇴에서 서버 측 세션 폐기와 유사한 효과를 낼 수 있다.

## 결과

- 토큰 갱신 요청마다 기존 토큰 상태 변경과 새 토큰 저장이 필요하다.
- Refresh Token 조회는 원문이 아니라 해시로 수행한다.
- 만료, 재사용, 폐기 상태 변경은 감사와 디버깅에 도움이 된다.
- 토큰 상태 변경 직후 예외를 던지는 경우 트랜잭션 롤백 여부를 주의해야 한다.

## 대안

- Refresh Token 원문 저장: 구현은 단순하지만 DB 유출 시 위험이 크다.
- Refresh Token을 저장하지 않는 완전 stateless 방식: 로그아웃, 탈퇴, 강제 폐기 처리가 어렵다.
- Redis에 Refresh Token 저장: 빠른 조회와 TTL 처리가 가능하지만 현재는 MySQL 기반 MVP로 시작한다.
