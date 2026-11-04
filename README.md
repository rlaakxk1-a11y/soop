# SOOP Viewer 0.2.0 — native Android viewer

비공식 개인용 SOOP 뷰어. 홈·검색·방송국·채팅과 영상 플레이어는 Android 네이티브 화면입니다. 공식 로그인과 재생 실패 시 사용자가 선택하는 공식 화면만 WebView를 사용합니다. Android 8.0+, compile/target SDK 35, Java 17, Gradle 8.9. Galaxy S23+ 실기기 검증 전입니다.

## 구현
- 인기 LIVE 방송 썸네일 목록, 페이지 추가 로드
- LIVE / VOD 검색
- 방송국 ID 직접 열기, 방송 카드에서 방송국 방문, 기기 내 방송국 즐겨찾기
- 방송국의 다시보기 / 전체 VOD / 클립 목록
- AndroidX Media3 앱 영상 플레이어: 생방송 HLS, VOD 다중 파일, 탐색·일시정지·가로화면
- 실제 영상 트랙의 화질 선택 / 자동 화질
- 별도 네이티브 읽기 전용 채팅 패널: 숨김, 상하좌우 분할, 자유 이동, 손잡이 드래그, 모서리 크기 조절, 설정 저장
- 생방송 채팅 WebSocket 수신, VOD 파일별 채팅 XML 구간 로딩과 현재 파일의 재생 위치에 맞춘 표시
- MY → 공식 SOOP 로그인, 앱 전용 CookieManager 세션 유지, 세션 삭제
- `.com`과 `.co.kr` 공식 링크 입력 지원
- 일부 타사 광고 호스트 차단 토글 (자체 영상 광고 제거 기능 아님)

## 로그인 및 권한
앱에 아이디/비밀번호를 제공하거나 소스에 넣지 않습니다. 본인이 SOOP 공식 로그인 화면에 입력합니다. 계정의 성인 인증과 시청 권한은 서버가 판단하며, 인증 완료를 위조하거나 제한을 우회하지 않습니다. 로그인 세션 쿠키는 앱 전용 웹 저장소에 남고, 네이티브 요청은 CookieManager가 해당 요청 호스트에 허용하는 쿠키만 전송합니다. API 응답의 Set-Cookie도 동일 저장소로 전달합니다. 비밀번호/세션/재생 토큰을 소스·테스트·로그에 기록하지 않습니다. 앱 데이터 백업은 비활성화했습니다.

소셜 로그인, 외부 본인 인증 앱, CAPTCHA, 세션 만료, 성인 인증의 실제 적용은 검증 전입니다. 외부 인증을 브라우저로 열면 그 세션이 앱에 자동 공유되지 않을 수 있습니다. 로그인 상태 표시는 ‘웹 세션 존재’만 뜻하며 로그인이나 성인 인증 성공을 단정하지 않습니다. 구독자 전용 VOD, 비밀번호 방송, 별도 권한 발급이 필요한 콘텐츠는 공식 재생 화면으로 확인하도록 안내합니다.

## 검증 범위와 한계
클라우드의 지정 HTTP 프록시는 연결에 실패하고 SOOP 도메인이 허용 목록에 없어 실제 서비스에 접속하지 못했습니다. 구현은 공개된 연동 자료의 API 형식에 근거하며, 현재 라이브 응답·실제 로그인·영상 재생·채팅 연결은 검증 전입니다. SOOP 공식 앱의 모든 기능을 구현한 것은 아닙니다. 서비스 API 변경 시 실패할 수 있으며 이때 오류 안내를 표시합니다. 채팅 전송, 선물/스티커 전송, 서버 계정 즐겨찾기 동기화, 오프라인 다운로드는 구현하지 않았습니다. 현재 즐겨찾기는 기기 내 방송국 목록입니다.

채팅 XML 구간은 최대 8개만 메모리에 보관하고 VOD 탐색 시 해당 구간을 불러옵니다. 구간 로딩 중에는 잠깐 표시가 늦어질 수 있습니다. 영상 위치 주변 최근 60초의 채팅을 최대 150개 표시합니다. 생방송 채팅은 최대 200개 표시합니다. 네트워크 오류 후 자동 무한 재접속하지 않으며 화면을 다시 열어 재시도합니다.

## APK 설치
PR의 Android APK 빌드가 성공하면 `soop-viewer-apk` 아티팩트 ZIP 안에 `app-debug.apk`가 있습니다. GitHub 로그인 후 다운로드 → ZIP 압축 풀기 → APK 선택 → 설치. 개발용 서명이며 Play Store 배포본이 아닙니다. 이전 0.1.0 APK와 서명 키가 다르면 먼저 이전 앱을 제거해야 합니다 (웹 세션과 설정 삭제). 이후 CI에서는 개발용 키 캐시를 사용하지만 캐시가 사라지면 키가 바뀔 수 있습니다.

## 빌드 및 테스트
JDK 17+, Gradle 8.9, Android SDK Platform 35 / Build Tools 35.0.0:

```bash
export ANDROID_HOME=/path/to/Android/Sdk
gradle --no-daemon testDebugUnitTest assembleDebug lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

로컬 환경은 JDK 컴파일러·SDK·Gradle 부재와 프록시 연결 실패로 빌드할 수 없어 연결된 GitHub Actions에서 수행합니다. Gradle wrapper는 포함하지 않았습니다.

테스트: 목록/VOD 응답 파싱, 인증 필요 응답, URL/광고 호스트 경계, 채팅 XML/UTF-8 프레임/잘린 패킷, XML 엔티티 차단, 네이티브 홈과 채팅 분할 레이아웃. Robolectric UI 테스트의 API는 오프라인 fixture이며 실제 SOOP에 요청하지 않습니다. CI는 SOOP 실서비스를 테스트하는 용도로 사용하지 않습니다.

## 연동 형식 참고
API/프로토콜 형식을 확인하기 위해 아래 공개 자료를 참고했으며 네트워크·UI·파서는 이 프로젝트에서 작성했습니다.
- https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/afreecatv.py
- https://github.com/power721/alist-tvbox/blob/master/src/main/java/cn/har01d/alist_tvbox/live/service/SoopService.java
- https://github.com/liuchuancong/pure_live_TV/tree/main/lib/platforms/soop
- https://github.com/Khassarion/VOD-Master/blob/main/src/module/soop_api.js
