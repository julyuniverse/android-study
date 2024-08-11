# Android Study - My development record

최초 작성일: 2024-08-02

### google login

- 작성일: 2024-08-02
- 내가 개발한 google login은 google id 인증이 android -> google 서버 -> android가 아닌 android -> 나의 서버(내가 개발한 서버) -> android 방식으로 인증 처리하도록 한다.
- 서버에서 인증 처리하도록 하려면 Google Cloud Platform (GCP)에서 OAuth 2.0 클라이언트 ID 생성할 때 android용과 web application용 2개 모두 만들어야 된다.
- android studio 실행 환경이 달라질 경우 android용 SHA-1 서명 인증서 디지털 지문을 그 환경의 SHA-1 서명 인증서 디지털 지문으로 변경해야 한다.

### Server reference

https://github.com/julyuniverse/spring-boot-study/tree/main/social-login

### Developments

- apple login으로 회원가입 또는 로그인 처리되고 그 이후 api 통신은 서버에서 자체 발행한 토큰으로 통신해요.
