# ADR-0004: 모듈 간 JPA Entity 참조보다 ID 참조를 우선한다

## 상태

Accepted

## 날짜

2026-05-08

## 맥락

모듈러 모놀리스에서 각 모듈은 독립적인 책임과 도메인 모델을 가져야 한다. 다른 모듈의 JPA Entity를 직접 참조하면 모듈 간 결합도가 높아지고, 변경 영향이 쉽게 전파된다.

예를 들어 market의 Listing이 member의 User Entity를 직접 참조하면 member 내부 모델 변경이 market에 영향을 줄 수 있다.

## 결정

모듈 간 관계는 JPA Entity 직접 참조보다 ID 참조를 우선한다.

다른 모듈의 기능이 필요한 경우 공개된 QueryService 또는 CommandService를 통해 호출한다.

## 이유

- 모듈 간 결합도를 낮출 수 있다.
- 각 모듈의 내부 도메인 모델을 독립적으로 변경하기 쉽다.
- Spring Modulith의 모듈 경계 검증과 잘 맞는다.
- 향후 일부 모듈을 서비스로 분리할 때 전환 비용을 줄일 수 있다.

## 결과

- 다른 모듈의 Entity를 직접 필드로 참조하지 않는다.
- 외래키가 필요하더라도 코드 레벨에서는 ID 값을 우선 사용한다.
- 모듈 외부에 공개할 기능은 QueryService 또는 CommandService 계약으로 제공한다.
- 내부 Entity, Repository, Service 구현체는 모듈 내부 패키지에 둔다.

## 대안

- JPA 연관관계를 적극 사용하는 방식: 조회 편의성은 있지만 모듈 간 결합이 강해져 선택하지 않았다.
