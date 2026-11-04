# SOOP Viewer Android

개인용 비공식 SOOP WebView 뷰어. 공식 사이트를 앱 안에서 열고, 원래 채팅 DOM을 복사하지 않고 위치와 크기를 변경합니다. Android 8.0 이상, compile/target SDK 35, Java 17, Gradle 8.9. S23+용 별도 ABI 설정이 필요하지 않습니다.

## 기능
- 첫 실행에 공식 SOOP 모바일 사이트 연결
- 생방송 / 다시보기 / 검색 / 공식 링크 입력
- 채팅 숨김 및 15–65% 크기 슬라이더
- 메뉴 → 채팅 위치: 자동 / 왼쪽 / 오른쪽 / 위쪽 / 아래쪽 / 자유 이동
- 채팅 위 ‘채팅 이동’ 손잡이 드래그, 오른쪽 아래 모서리 드래그로 크기 조절
- 늦게 로드되는 채팅 자동 감지, 창 크기 변경 시 화면 밖으로 나가지 않도록 제한
- 메뉴 → 광고 차단 켜기/끄기: 일부 타사 광고 호스트 요청과 명시적 광고 배너 숨김
- 오프라인 레이아웃 데모, 뒤로 가기, 웹 영상 전체화면

## 한계
실제 SOOP 페이지는 현재 클라우드 허용 목록에 없어 검사하지 못했습니다. 채팅 셀렉터와 검색 URL, 생방송/VOD 재생, 로그인은 실서비스 검증 전입니다. 사이트 구조에 따라 채팅이 감지되지 않거나 플레이어와 겹칠 수 있습니다. 크로스 오리진 iframe 내부 채팅은 제어하지 못합니다. 전체화면 영상에서는 웹 채팅이 표시되지 않을 수 있습니다. 기본 배치는 화면 위에 채팅을 띄우는 방식입니다.

광고 차단은 doubleclick.net, googlesyndication.com, googleadservices.com 및 일부 배너 CSS에 한정됩니다. SOOP 자체 광고나 스트림에 삽입된 영상 광고까지 제거하는 기능은 아닙니다. 재생 장애 시 메뉴에서 차단을 끄세요. 유료/연령 제한과 로그인은 공식 서비스의 인증을 사용합니다.

## APK 받기 / 설치
GitHub Actions의 `Android APK` 실행이 성공하면 `soop-viewer-apk` 아티팩트 ZIP 안에 개발용 서명된 `app-debug.apk`가 있습니다. 휴대전화에서 ZIP을 풀고 APK를 선택해 설치하세요. 해당 다운로드 앱의 ‘출처를 알 수 없는 앱 설치’를 허용해야 할 수 있습니다. Actions 아티팩트 다운로드는 GitHub 로그인이 필요합니다.

CI마다 개발용 서명 키가 달라질 수 있으므로 이후 업데이트 시 서명 충돌이 발생하면 기존 앱을 제거하고 설치하세요 (저장된 웹 로그인과 설정 삭제).

## 로컬 빌드
JDK 17+, Gradle 8.9, Android SDK Platform 35 / Build Tools 35.0.0을 준비한 뒤:

```bash
export ANDROID_HOME=/path/to/Android/Sdk
./build-apk.sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Gradle wrapper는 포함하지 않았습니다. GitHub Actions가 도구를 설치하고 빌드·lint·서명 검증을 수행합니다.

## 테스트
```bash
npm install --no-save --package-lock=false jsdom@26.1.0
node --test tests/chat-layout.test.cjs
mkdir -p work/test-classes
javac -d work/test-classes app/src/main/java/dev/soopviewer/PanelState.java app/src/main/java/dev/soopviewer/AdRules.java tests/RulesTest.java
java -cp work/test-classes RulesTest
```
JS DOM 테스트는 늦은 채팅 감지, 숨김/복원, 위치/범위, 중복 주입, 광고 설정, 창 밖 좌표 제한을 확인합니다. Java 테스트는 신뢰 호스트와 광고 호스트의 경계 등을 확인합니다. 실기기 검증은 별도입니다.

로컬 환경은 JDK 컴파일러/SDK/Gradle 미설치와 지정 프록시 연결 실패로 빌드하지 못했습니다. CI 결과는 Actions에서 확인하세요.
