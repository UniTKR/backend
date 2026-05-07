# ADR-0003: UniT의 모듈 경계를 정의한다

## 상태

Accepted

## 날짜

2026-05-08

## 맥락

UniT는 여러 기능 영역을 포함한다. 기능이 늘어날수록 패키지 의존성이 섞이면 변경 영향 범위가 커지고, 나중에 모듈 분리나 유지보수가 어려워진다.

초기부터 명확한 모듈 책임을 정의해 모듈러 모놀리스 구조를 유지한다.

## 결정

UniT 백엔드는 다음 모듈로 구성한다.

- platform: 기술 공통, 공통 응답, 공통 에러, 보안 기반, 알림, idempotency, observability
- member: 사용자, 학교, 자체 로그인, 소셜 로그인, 학교 이메일 인증, 신뢰 점수
- market: 판매글, 상품, 카테고리, 이미지, 관심/찜
- trade: 거래, 채팅, 채팅 차단, 만남 장소, 후기
- ops: 신고, 관리자, 사용자 제재

## 이유

- 기능 책임이 명확해진다.
- 모듈별 변경 영향 범위를 줄일 수 있다.
- Spring Modulith를 통해 모듈 의존성을 검증할 수 있다.
- 향후 일부 영역을 독립 서비스로 분리할 때 기준이 된다.

## 결과

- chat은 trade 모듈에 포함한다.
- campus 모듈은 따로 만들지 않고 member와 trade에 나누어 배치한다.
- trust 모듈은 따로 만들지 않고 review는 trade, score는 member에 둔다.
- notification과 idempotency는 platform에 둔다.
- moderation과 admin은 ops로 합친다.
- 채팅 차단은 trade, 사용자 제재는 ops의 책임으로 구분한다.

## 대안

- 기능별로 더 잘게 모듈을 나누는 방식: 초기 복잡도가 커져 선택하지 않았다.
- 모든 기능을 하나의 application/domain/infra 구조로 두는 방식: 모듈 경계가 약해질 수 있어 선택하지 않았다.
