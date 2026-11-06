# Mobile playback and browsing, 0.8.0

## Evidence and live playback

The previous release requested desktop html5 AIDs and mapped domestic CDNs to PC web CDNs. That does not establish a working high-quality mobile stream. Public reports describe an identical 540p symptom for Korean desktop HLS clients: https://github.com/streamlink/streamlink/issues/6563 .

Inspected SOOP Android 8.34.4, package kr.co.nowcom.mobile.afreeca, downloaded from the Aptoide mirror (https://afreecatv-tv.kr.aptoide.com/app). The Play listing identifies the same version: https://play.google.com/store/apps/details?id=kr.co.nowcom.mobile.afreeca . APK code/resources are research references only; no official application code, native libraries, or artwork are included in this viewer.

On 2026-11-06, unauthenticated requests to the official mobile watch API returned separate web-HLS and cast credentials and separate CDN assignments. The web credential produced a single 960x540 stream. The server-issued cast credential produced a master playlist containing original/hd4k/hd/sd variants. The implementation uses cast only when the API advertises is_chromecast and supplies both a cast key and CDN; otherwise it uses the web entitlement. It never sets an adult-confirmation flag, forges cookies, invents a token, forces another region/CDN, or falls back after an authorization error.

The selected variant URI comes directly from the returned master playlist. Its own signed aid query must not be replaced by the master key. Relative paths and all signed query parameters are preserved. Automatic mode can retain the master. The button still reports decoded resolution, and the quality sheet distinguishes selected quality from actual playback.

Live media segments, independently checked with ffprobe:

| Streamer | Broadcast | Actual video |
|---|---|---|
| phonics1 | 298377459 | H.264, 1920x1080 |
| lshooooo | 298359665 | H.264, 1920x1080 |
| aflol | 298384167 | H.264, 1920x1080 |

These are network/media checks from the build environment, not a claim of Samsung device verification or of availability for every account, region, or restricted broadcast. Tokens and media URLs are intentionally omitted. The previous web chat handshake remains independent of mobile playback authentication.

## Browsing changes

- Persistent mobile header with search and MY; active bottom navigation.
- Home tabs for explore/recommendation/live/history, horizontal streamer portraits, large recommended broadcast cards and compact live listings.
- Category discovery uses the official mobile category API and filters live lists; search remains native.
- Native channel profile, favorites, home, videos, replays, clips and posts. Public channel posts use the current api-channel service; HTML is rendered as selectable text, with supported post photos.
- Channel VOD thumbnails and durations read the current nested ucc response instead of falling back to a profile image.
- No feed advertisements, promotional banners or invented chat previews. Known ad domains remain filtered. No change to server-side advertising entitlements is claimed.
- Existing history, in-app mini player, Android PiP and draggable/resizable chat are retained.

This implements the viewer's browsing/playback screens with the official mobile structure as reference. It is not the complete official application: broadcasting, gifts/payments, account favorites synchronization, chat posting, esports portals and the Catch feed are not reimplemented. Existing read-only chat behavior is retained.

## Validation and signing

Regression tests cover separate cast/web entitlements, preserved signed variant queries, missing qualities, authorization failures, mobile preset names, nested VOD thumbnails/duration, channel tabs, category navigation, and existing chat/PiP/history/layout cases. Offline Robolectric screenshots provide layout evidence using fixture content. Android CI runs testDebugUnitTest, assembleDebug, lintDebug and apksigner verification.

Application ID stays dev.soopviewer. Version becomes 0.8.0 / versionCode 12. Existing soop-viewer-debug-signing-v2 cache is restored without generating a key. The expected certificate SHA-256 remains 9810f69eebd819e4ba703f5953527789717bf570be043bb2c85c9cdee669f54f.
