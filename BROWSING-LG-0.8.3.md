# 0.8.3: connected browsing controls and LG high-quality recovery

## Controls audited and connected

| Control | Actual effect |
|---|---|
| Live order | Server orderType=view_cnt / view_cnt_asc / broad_start. These values were checked against live API responses; low-viewer sorting starts from the server's low-viewer page, not a reversed high-viewer page. |
| Category + order | Keeps selected category, requests its page 1 with the new order; “전체” clears category while retaining order. |
| Search tabs and order | Active tab styling, retained query and type, actual szOrder=score/reg_date/view_cnt requests. |
| Channel video tabs and order | Retains all/review/clip tab and changes orderby=reg_date/reg_date_asc/read_cnt. view_cnt is not used because this endpoint ignored it during probing. |
| Load more / retry / refresh | Prevents duplicate concurrent next-page loads, commits page after success, retries failed page without clearing existing results, removes overlapping items while preserving server order. |
| Favorites | Updates the live star immediately without restarting playback; channel toggle and saved channel navigation remain connected. Favorites list scrolls; its search action explicitly selects streamer search. |
| Mini-player order | Changes the browse query without releasing the currently playing mini-player. |
| Recommended quality / explicit quality | Recommended quality actually requests a fresh high-quality route even if selectedQuality still says original after a lower startup fallback. Explicit high-quality selection no longer silently succeeds with a web-only lower rendition. |

Existing history, native login/navigation, chat placement/size controls and Android PiP are retained. Unknown official-app features are not presented as newly implemented by this change.

## LG high-quality playback

User diagnostic: lg_cdn_mobile_web returned HTTP 400 for the primary cast master, then web-only 540p was used. Global-CDN tests cannot establish a fix for this specific client condition.

The resolver now tries a bounded set of server-issued high-quality routes before startup web fallback: primary cast master, the exact requested preset directly (with the supplied resource manager), then a distinct server-issued spare cast credential. It never fabricates credentials, spoofs region/adult status, forces another CDN family, or retries through other routes after HTTP 401/403. Explicit 1080p selection reports failure and retains current playback if these high-quality routes fail.

LG mobile-web transport was separately checked using a public live broadcast and ffprobe. Primary-master/original, direct-original and spare-master/original each produced actual H.264 1920x1080 segments from the LG delivery host. The cloud request's watch response selected a global CDN; these LG transport checks are not a reproduction of the user's Korean handset IP/account/session. Success on that handset remains unverified. Server denial or a lower server-supplied payload is not relabeled as successful 1080p.

Diagnostics now use BuildConfig.VERSION_NAME, identify primary/direct/spare route and delivery hostname, and record actual decoded video size. Cookie/token/media query values remain excluded.

## Validation

New tests cover real API sort parameters, actual dialog actions changing requests and displayed results, category preservation, saved order, VOD/search sorting, pagination retry/deduplication, favorite state, direct-preset and spare-key recovery, and strict manual 1080p failure. Existing native login, chat, PiP, history and navigation tests remain in CI. Signing still requires the original retained Actions key. VersionName 0.8.3 / versionCode 15.
