# 0.8.1: recover live playback from HTTP 400

The 0.8.0 live resolver passed cdn_chromecast straight to broad_stream_assign. A native CDN family such as gs_cdn selects chromecast.stream.sooplive.com, not the mobile browser HLS endpoint. A request to that endpoint with a mobile-watch credential was reproduced returning HTTP 400. Global gcp_cdn had worked in the preceding tests and did not exercise this domestic delivery path. This is evidence of a routing bug; it is not a claim that the user's account requires login.

Known native/PC gs_cdn and lg_cdn names now map to their mobile_web delivery endpoints for this HLS client. Unknown, global, and subscription-specific assignments remain unchanged. The resolver uses only credentials actually issued by the watch API; it never spoofs country, adult confirmation, or login.

A successful master fetch is insufficient: the selected signed child playlist is also checked before starting ExoPlayer. A cast-route transport or invalid-manifest failure can fall back to the independently issued web HLS credential. HTTP 401/403 and rejected watch authorization do not try another route. Web fallback may provide only 540p; decoded resolution remains the displayed truth.

A later player error can refresh the mobile session and retry the web route once, preserving the existing player, chat connection, UI and PiP. There is no infinite reconnect loop.

Errors identify the failed stage (watch information, CDN assignment, master playlist, child playlist). A diagnostic-copy action contains only route names, validated CDN names and error classes/status codes. It does not contain cookies, tokens, media URLs or server response bodies. The old overlapping error/status/buttons were replaced with one error panel.

VersionCode 13 / versionName 0.8.1. Existing signing cache and expected original certificate are unchanged. UI from 0.8.0 is retained. Regression coverage includes domestic mapping, cast/child HTTP 400, malformed manifests, server-assignment failures, no retry after authorization rejection, safe diagnostics, fresh web recovery and error-panel layout.
