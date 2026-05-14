# ADR-0012: 활성 회원 unique 제약은 generated column으로 처리한다

## 상태

Accepted

## 날짜

2026-05-14

## 맥락

UniT는 회원 탈퇴를 hard delete가 아니라 soft delete로 처리한다. 따라서 탈퇴 회원 row는 `users` 테이블에 남고 `deleted_at` 값으로 활성 여부를 구분한다.

이 상태에서 `email_hash`, `phone_hash`, `nickname`에 단순 unique 제약을 두면 탈퇴한 회원의 값 때문에 같은 이메일이나 닉네임을 다시 사용할 수 없다.

서비스 정책상 탈퇴 회원의 닉네임과 이메일 재사용 가능 여부는 별도 결정이 필요하지만, 최소한 활성 회원끼리의 중복만 막을 수 있는 DB 구조가 필요하다.

## 결정

MySQL generated column을 사용해 활성 회원만 unique 제약 대상이 되도록 한다.

`deleted_at IS NULL`인 경우에만 값이 채워지는 `active_email_hash`, `active_phone_hash`, `active_nickname` 컬럼을 만들고, 이 generated column에 unique index를 건다.

탈퇴 회원은 generated column 값이 `NULL`이 되므로 활성 회원 unique 제약에서 제외된다.

## 이유

- MySQL은 PostgreSQL의 partial unique index와 같은 문법을 직접 제공하지 않는다.
- generated column을 사용하면 MySQL에서도 조건부 unique 제약과 유사한 효과를 낼 수 있다.
- 애플리케이션 검증뿐 아니라 DB 제약으로도 활성 회원 중복을 막을 수 있다.
- 탈퇴 회원 row를 보존하면서도 활성 회원 기준 unique 정책을 유지할 수 있다.

## 결과

- 활성 회원끼리는 이메일, 전화번호, 닉네임 중복이 DB 레벨에서 차단된다.
- 탈퇴 회원의 값은 unique 제약 대상에서 제외된다.
- 애플리케이션 코드의 중복 검증과 DB 제약이 같은 정책을 바라봐야 한다.
- generated column 정의가 DB 종속적이므로 다른 DB로 이전할 때 재검토가 필요하다.

## 대안

- 단순 unique 제약 유지: 탈퇴 회원 값 때문에 재가입과 닉네임 재사용이 제한된다.
- unique 제약 제거 후 애플리케이션에서만 검증: 동시성 상황에서 중복 삽입 위험이 있다.
- PostgreSQL partial unique index 사용: 기능은 적합하지만 현재 DB 선택은 MySQL이다.
