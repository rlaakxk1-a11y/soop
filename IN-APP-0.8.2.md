# 0.8.2: native account screens and in-app navigation

The previous login action opened SOOP's entire website in a WebView and left its home page embedded after login. This release replaces the default login UI with native Android inputs that submit directly to SOOP's existing official HTTPS login endpoint. Password text is cleared after submission, excluded from view-state saving, and not written to preferences or diagnostic logs. Session cookies continue to use the same CookieManager bridge as playback.

Server RESULT=1 is followed by a session identity check using the official get_private_info endpoint. A redirect URL or existing cookie alone does not prove login. Invalid credentials remain on the native form. Additional verification, social login and account recovery use an isolated in-app authentication WebView; they do not launch an external browser. Recognized HTTPS identity providers and authentication popup windows are supported, subject to provider WebView restrictions. No CAPTCHA, 2FA or authentication checks are bypassed, and real-account authentication has not been exercised in this environment.

After authentication, the embedded web home is intercepted and replaced with native MY, or with the original broadcast if login started while watching. Failed/cancelled authentication does not claim success. Ordinary home, category, search, channel, live and VOD links dispatch to the existing native app screens, including .com/.co.kr and mobile hash-player routes. Unknown routes are not disguised as implemented native features.

The app can receive supported HTTPS links through an Android VIEW intent filter. This is not verified App Links ownership; Android may ask the user which application should open a link.

Existing 0.8.1 media routing, web fallback, chat dragging/resizing, PiP and original signing key remain unchanged. VersionName 0.8.2 / versionCode 14. Regression tests cover routes, host boundaries, native login, password handling, server-confirmed completion, the embedded-home regression, cancellation and native channel navigation. Offline screenshots include the empty native login form. No real credentials are used by tests or included in source.

Protocol reference inspected: https://login.sooplive.com/login_script.php (LoginAction.php form fields and RESULT values). Session response shape also matches the Streamlink SOOP client: https://github.com/streamlink/streamlink/blob/master/src/streamlink/plugins/soop.py .
