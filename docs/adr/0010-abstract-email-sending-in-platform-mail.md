# ADR-0010: 메일 발송은 platform mail 인터페이스로 추상화한다

## 상태

Accepted

## 날짜

2026-05-14

## 맥락

학교 이메일 인증은 `member` 도메인의 기능이지만 실제 SMTP 발송, HTML 템플릿 렌더링, JavaMail 설정은 특정 도메인보다 기술 공통 성격이 강하다.

`member`가 JavaMailSender나 SMTP 설정에 직접 의존하면 도메인 코드와 인프라 코드가 섞이고, 테스트에서 실제 메일 발송 여부를 제어하기 어려워진다.

또한 Spring Modulith 구조에서는 모듈 간 의존을 공개된 인터페이스로 제한해야 한다.

## 결정

메일 발송 기능은 `platform.mail` 패키지에 두고 `EmailSender` 인터페이스를 공개한다.

운영 또는 로컬 실발송 환경에서는 `JavaMailEmailSender`를 사용하고, 메일 발송이 비활성화된 환경에서는 `NoOpEmailSender`를 사용한다. HTML 메일 본문은 resources 하위 템플릿 파일을 읽어 렌더링한다.

`member` 모듈은 `EmailSender` 인터페이스만 호출하고 JavaMail 구현체를 직접 알지 않는다.

## 이유

- 도메인 로직과 메일 인프라 구현을 분리할 수 있다.
- 테스트와 로컬 환경에서 메일 발송을 끄더라도 `EmailSender` 빈은 항상 주입 가능해야 한다.
- HTML 템플릿을 resources에 두면 코드와 메일 마크업을 분리할 수 있다.
- `platform.mail`을 NamedInterface로 공개하면 Spring Modulith 모듈 경계를 유지할 수 있다.

## 결과

- `member`는 메일 발송 구현이 아니라 인터페이스에만 의존한다.
- SMTP 설정이 잘못되어도 테스트 환경에서는 NoOp 구현으로 컨텍스트를 띄울 수 있다.
- 실제 메일 발송 테스트는 로컬 수동 확인 또는 별도 통합 테스트로 분리한다.
- 향후 SMTP 대신 외부 메일 API를 사용해도 `EmailSender` 구현체만 교체하면 된다.

## 대안

- `member`에서 JavaMailSender를 직접 주입한다: 구현은 빠르지만 모듈 경계와 테스트 격리가 약해진다.
- 메일 발송을 notification 도메인으로 미룬다: 장기적으로 가능하지만 Sprint 1 학교 인증 구현에는 범위가 크다.
- 메일을 발송하지 않고 코드만 로그로 남긴다: 개발 초기에는 편하지만 실제 학교 인증 플로우 검증이 어렵다.
