# ADR-0002: WebFlux 대신 Spring MVC를 사용한다

## 상태

Accepted

## 날짜

2026-05-08

## 맥락

UniT는 일반적인 CRUD, 인증, 판매글 조회, 거래 상태 변경, 채팅 API를 중심으로 하는 서비스다. 초기 백엔드는 MySQL과 JPA를 사용하며, 대부분의 요청은 동기적인 데이터베이스 접근을 포함한다.

WebFlux는 높은 동시성과 논블로킹 I/O에 장점이 있지만, JPA 기반 애플리케이션에서는 그 장점을 온전히 살리기 어렵다.

## 결정

초기 API 서버는 Spring MVC를 사용한다.

## 이유

- JPA, Spring Security, MockMvc, REST Docs와 자연스럽게 맞는다.
- 팀 학습 비용과 구현 복잡도가 WebFlux보다 낮다.
- UniT의 초기 요구사항은 Spring MVC로 충분히 처리 가능하다.
- 동기식 요청 처리 모델이 디버깅과 테스트에 단순하다.

## 결과

- HTTP API는 Spring MVC Controller 기반으로 작성한다.
- 테스트는 MockMvc를 기본으로 사용한다.
- API 문서는 Spring REST Docs와 MockMvc 테스트를 통해 생성한다.
- 향후 실시간성이 필요한 영역은 별도 기술을 검토할 수 있다.

## 대안

- WebFlux: 논블로킹 처리에 유리하지만 JPA 기반 구조와 초기 개발 난이도를 고려해 선택하지 않았다.
