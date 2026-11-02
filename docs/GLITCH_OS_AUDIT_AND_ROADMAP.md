# Glitch OS audit and advancement roadmap

## Scope and status

This is the first audit pass of the Android app in `app/` (147 Kotlin files at the time of this pass). It covers the app's routed screens, tool families, Android integrations, API/data layers, automation paths, and obvious demo/fabricated-output paths. It is not a claim that every line of the roughly 52k-line source tree has had a full manual review.

**Status:** live-data and data-integrity pass in progress. No successful Android build or test run is claimed: the current environment does not have an Android SDK configured. The 100 advancements below are a staged plan; they are not represented as already implemented.

## App and tool inventory

### User-facing screens and tools

| Area | Available tools and functions | Data / integration |
| --- | --- | --- |
| Home and navigation | Home dashboard, app navigation, update prompt, quick dock | Local settings, app state, update metadata |
| Video creation | Topic/category/language/duration selection, script and scene planning, captions, voice selection, music/SFX, thumbnail prompts, native video generation | Native pipeline and configured AI/media services; failures should be surfaced, not replaced with sample assets |
| Generation lifecycle | Progress, notifications, preview/player, project details, logs | WorkManager, Room, device storage, FFmpeg |
| Library | Browse and open saved projects, preview, details | Local project database and file storage |
| Trend Pilot | YouTube most-popular chart feed with observed title, rank, counts, and publish date | Google OAuth and YouTube Data API; requires real sign-in/scopes and network access |
| YouTube manager | Channel/profile switching, channel/video analytics, comments and replies, quota ledger, community-post drafts, playlists, SEO, watchdog diagnostics, live trend scan, scheduled content queue, autopilot controls | YouTube Data API/OAuth; some optional AI functions use configured providers |
| Chatbot | Creator assistant conversation and preview handoff | Configured AI provider |
| Shorts player | Vertical YouTube video browsing | YouTube/app integration |
| Keyboard playground / custom IME | QWERTY layouts, voice input, suggestions, clipboard, snippets, calculator, cursor pad, emoji/symbols, text transforms, auto-type/repeater/macros, settings, AI panel | Android IME, microphone/speech services, local preferences; generated examples are local convenience tools, not live account data |
| VPN Shield | VPN connection state, server selection/rotation and autopilot gate | Android `VpnService`; user consent and network availability required |
| Ad Shield | Accessibility-based skip-button actions, mute controls, Shorts/music/browser toggles, floating overlay | Android accessibility/overlay permissions; counters must only reflect observed actions |
| Settings and permissions | App preferences, logs, update information, background-permission guidance | Local preferences, Android permission state, configured Firebase update data |

### Application/service catalog

| Package / files | Responsibility |
| --- | --- |
| `ui/screens/` (14 Kotlin files) | Home, create, progress, preview, library, details, settings, logs, login, YouTube manager, Trend Pilot, keyboard playground, VPN Shield, chatbot |
| `ui/components/` (10 components) | Shared UI, update/permission/compliance cards, thumbnail editor, player view, home dock, YouTube ad-shield controls |
| `ui/keyboard/` (25 Kotlin files) | Custom IME service, key/layout/state models, smart bar, voice, clipboard, snippets, macros, cursor, calculator, emoji/symbols, AI and settings panels |
| `ui/viewmodels/` | Main app state, project generation and app-level actions |
| `data/local/` (16 files) | Room entities/DAOs/databases for projects, settings, auth, queues, channels, logs and agent preferences |
| `data/network/`, `data/repository/`, `data/youtube/`, `data/model/` (10 files) | API client, video/Firestore repositories, upload client, API and domain models |
| `pipeline/` (17 files) | Video request/pipeline, NoTrack and DeepAI WebView integrations, voice/transcription, FFmpeg rendering, DOM/HTML animation/rendering and execution coordination |
| `util/` (33 files) | Google sign-in/YouTube reads, Firestore, queue/watchdog/learning, script/SEO/thumbnail tools, music/SFX/captions/transitions, TTS/image services, cache, device/network and sharing helpers |
| `worker/` (3 files) | Video automation and autonomous-channel background work plus scheduling; video uploads are performed by the active video automation worker |
| `vpn/`, `service/`, `receiver/` | VPN service/gates, accessibility and floating overlay services, boot and VPN-action receivers |
| `notification/`, `auth/`, `ytplayer/`, app entry points | Pipeline notifications, login activity, Shorts player and application/activity startup |

### External and Android integrations

- Google Sign-In/OAuth and YouTube Data API for account identity, chart, channel/video data, comments/playlists, and uploads.
- Firebase Authentication/Firestore for configured user/profile/upload/update persistence.
- Room and Android preferences for local projects, channel profiles, settings, quotas, logs, and queues.
- WorkManager for constrained/recurring automation and upload work.
- FFmpeg/native rendering, Android media/audio, configured image/AI services, Edge neural TTS, and Whisper transcription.
- Android accessibility, VPN, overlay, input-method, microphone, notification, and boot-receiver APIs.

Provider availability, credentials, scopes, Android version, and user-granted permissions determine which integrations actually work on a device.

## Confirmed first-pass fixes

| Finding | Resolution in this pass |
| --- | --- |
| Scene rendering could substitute Picsum/procedural/previous-frame images for absent or invalid input. | Removed the synthetic image fallbacks; missing or undecodable scene inputs fail explicitly. |
| OTP diagnostics substituted fictional log entries, source snippets, and network calls when real input was unavailable. | Removed all sample inputs and the demo tool listing; diagnostics now inspect only supplied real data and explain when input is missing. |
| An unused explorer view model exposed a simulated-data toggle while depending on missing repository/model types; its tool registry also advertised unsupported tools. | Removed the unreferenced, incomplete feature code rather than presenting nonexistent device scans as working functionality. |
| Trend Pilot displayed canned topics and made-up growth velocities. | Replaced with the live YouTube most-popular chart and observed video fields; failures are surfaced. |
| Create Video auto-filled canned category topics and hid provider failures behind template scripts. | Replaced topic suggestions with the live chart, requires a user/chart title, and reports provider failures without silently substituting scripts. |
| Channel diagnostics and performance screens could present fabricated metrics or unsupported CTR/retention diagnoses. | Removed fabricated diagnostic defaults and unsupported conclusions; missing metrics are identified as unavailable. |
| Learning helpers could imply invented traffic multipliers or canned topic advice. | Removed the fabricated multipliers and canned suggestions; recommendations now depend on observed data. |
| Queue generation depended on fabricated trend scores/hooks and a queue picker could select a future-dated READY item. | Queue topics now come from live chart titles, fabricated score/hook text is not shown as analytics, and DAO selection requires the schedule time to be due. |
| Automation scheduler and first-run paths could inject static sample topics or generic fallback topics. | Scheduler passes no invented topic; the worker uses a due queue item, an explicit topic, or a real YouTube chart title, and stops explicitly if none is available. |
| A “test simulation” action inflated ad-skip counters, and fixed 15-second estimates were shown as actual time saved. | Removed the simulation, removed the unsupported time-saved metric, renamed the counter as successful accessibility actions, and reset ambiguous historical counts once on migration. |
| Keyboard data generator inserted generated fake phone/card values into the active app. | Removed fake phone and mock card insertion actions. |
| Upload workers replaced missing/template titles with invented “Mind-Blowing Facts” titles and could report success after an upload failure. | Uploads now reject blank or known placeholder titles; automatic-upload failures and missing sign-in fail explicitly instead of being reported as completed. |
| A due queue item stayed `QUEUED` after successful generation when upload was disabled or failed, so later runs could regenerate the same item. | Queue entries move to `GENERATED` immediately after rendering succeeds and to `PUBLISHED` after upload succeeds; due selection only considers `QUEUED` entries. |
| Keyboard form macros inserted fictional personal details. | Replaced fabricated contact/address values with explicit fill-in placeholders. |
| Create and chatbot story actions could insert hardcoded topics when the user left the field blank. | They now require an explicitly supplied topic or a selected live-chart title. |
| An unrouted legacy dashboard contained simulated Appium success logs and a Picsum URL; it also housed a shared YouTube video model. | Removed the dead dashboard, extracted `VideoItemData` into the YouTube data package, and updated consumers. |
| An unused standalone YouTube upload worker duplicated the active upload flow and carried canned upload metadata defaults. | Removed the unreferenced worker; uploads continue through the wired video automation flow. |
| The chatbot substituted a canned sample story and scripted generic advice when providers were unavailable. | Removed both success-shaped fallbacks; provider unavailability is now shown explicitly. |
| YouTube comments UI displayed fictional audience comments and scripted reply claims; autonomous replies could be posted without the AI provider. | Removed sample comments and canned reply fallbacks; errors are surfaced and autonomous replies are skipped unless provider-generated suggestions are available. |
| YouTube comment fetch failures could be presented as an empty live feed, and background workers ignored retryable scan/reply failures. | Failed API requests now produce explicit load errors; retryable autonomous run failures are returned to WorkManager for retry. |
| SEO helpers implied CTR/viral performance, fabricated a checklist score, added Shorts tags to every video, and rendered planned timestamps as if they were analyzed frames. | Renamed generated titles as neutral drafts, replaced the score with a local metadata checklist, removed unconditional Shorts tags, and extract/review actual frames from the saved rendered video. |
| Pipeline metadata defaulted missing SEO scores to 95 and assigned 98 to a local template. | Missing pipeline SEO scores remain unset; only the separate local metadata checklist is calculated and labeled as such. |
| Script and visual-prompt provider failures could substitute large category-specific invented narratives/scenes; malformed provider output was silently completed with templates. | Removed those fallback generators; provider output must now pass required-field, placeholder, and scene-count validation or generation fails. |
| Speech synthesis could return a modulated tone, repeat narration, return English for unsupported selected languages, or mislabel compressed provider bytes as PCM. | Removed synthetic/repeated speech, map supported languages explicitly across cloud and device TTS, reject unsupported languages and undecodable provider audio, and fail when providers cannot produce valid narration. Fallback settings are labeled best-effort. |
| YouTube resumable upload initialization sent a masked placeholder instead of the caller's OAuth bearer token. | Send the supplied access token as a Bearer authorization header. |
| Pipeline retried all failures and could activate network/VPN actions for invalid provider output; invalid topics/durations were normalized to fabricated project-ID parts/default duration. | Pipeline validates topic/duration inputs, uses a random project ID, and only retries classified network failures. |
| Audio fallback preferences silently defaulted to enabled if reading settings failed. | BGM/TTS now read the persisted fallback setting directly instead of silently enabling fallbacks on preference errors. |
| The category topic catalog was only used by the removed dashboard. | Deleted the unused hardcoded topic catalog. |
| Create Video accepted blank topics through non-UI callers. | Added a ViewModel guard and visible error state; metadata prompt creation also rejects blank topics. |
| The completed-job action could navigate to preview with a fabricated title and empty video URL. | The action is only shown for a returned playable URL; missing output is called out explicitly. |
| Password generation used the default pseudo-random source. | Switched generated passwords to `SecureRandom`. |
| Project identity and basic README did not match the app. | Updated app metadata/name and README identity. |
| OkHttp logging could expose the authorization header. | Added authorization redaction to the logging configuration; no API credential value was changed. |
| Chatbot video generation could send an empty topic, use a fixed sample story, or silently substitute localhost/template scene animations. | The one-click action now requires the entered topic, requests and validates a configured 10-scene storyboard, and uses ten isolated parallel Notrack sessions; missing provider output fails explicitly. |
| Generated HTML could be duplicated when body markup already contained styles/scripts; capture could leave a late-created WebView alive; preview routing encoded local video URLs twice. | Animation markup is normalized, renderer WebViews are created/destroyed on Main with measured dimensions, and preview navigation now passes raw values for the route's single encoding step. |
| Chat success UI could render the entire generated HTML/CSS/JS package as a chat message, and free-text mentioning “DOM” could unexpectedly start generation. | Chat reports a concise video-ready result, and generation runs only from its explicit button. |
| The full chatbot path did not connect storyboard narration to speech/rendering or expose YouTube-style creation controls. | The pipeline now passes validated narration to neural TTS and the final renderer, and the chatbot exposes language, category, duration, voice, aspect-ratio, and caption controls. Output duration follows the selected total duration. |
| Completed chatbot videos were recorded in a separate Room table that the app's Library does not read, so they could disappear from the user's project list and had no saved storyboard/settings details. | The chatbot now saves a validated completed project into the Library's local project store, including the playable file, first-frame thumbnail, narration, ten scene prompts, and selected video-agent settings; online project refreshes retain these local DOM projects. |

## Known remaining audit items

These are intentionally visible follow-up work, not claimed as resolved:

1. Some separate script/chat/community flows still need a source/citation and claim-validation audit before treating generated factual content as reliable.
2. Existing installs may retain a previously saved `global_fallback_enabled=true`; changing a default does not migrate stored settings. Decide migration and user-visible opt-in behavior before enforcing strict mode.
3. Review upload retry/resume behavior, scheduler duplication/retries, and stale queue cleanup.
4. Review hardcoded provider endpoints, credentials/configuration boundaries, debug logging, permissions, and privacy/retention behavior in a focused security/privacy pass.
5. Complete the source-level dead-code and duplicate-helper review for remaining modules, including obsolete Appium-related persistence that must retain Room migration compatibility.
6. Build, unit tests, and device/integration tests remain unverified until an Android SDK, valid provider configuration, and required permissions are available.
7. The chatbot renderer captures eight local HTML animation frames per scene and loops that short sequence to fill the chosen scene duration; confirm visual quality, WebView behavior, FFmpeg support, and memory use on representative devices.
8. Ten independent Notrack sessions run concurrently as isolated WebViews, not visible browser tabs; provider concurrency/rate limits and real end-to-end generation still require device validation.
9. Completed chatbot projects can be reopened from the Library and retain their storyboard/settings, but interrupted generation does not yet persist stage checkpoints or support resuming midway through the pipeline.

## Phased plan: 100 advancements

Each item is a proposed advancement, not a promise that it is already implemented. Phases are sequenced to establish trustworthy data and tests before expanding automation.

### Phase 1 — Data integrity and truthful UI

**Acceptance:** no production screen presents fabricated provider metrics as live data; unavailable/error states identify the source and recovery action.

1. Finish replacing static topic suggestions with region-aware live YouTube chart data.
2. Attribute every displayed trend to provider, region, retrieval time, and rank.
3. Add freshness timestamps and stale-data badges to provider-backed screens.
4. Represent unavailable, private, disabled, and zero-valued metrics distinctly.
5. Add typed API error states for authentication, quota, permission, network, and provider failures.
6. Migrate saved global-fallback preferences to an explicit, user-visible policy.
7. Verify the removed dashboard's fake paths stay absent and decide the unused Appium Room schema's migration-safe lifecycle.
8. Add regression tests for blank-topic rejection across script, SEO, thumbnail, and metadata generation.
9. Label generated examples and heuristic suggestions as generated, not measured facts.
10. Add regression tests proving demo data is never returned by production data repositories.

### Phase 2 — Build, tests, and maintainability

**Acceptance:** CI builds the Android app and runs deterministic unit tests on every change; critical data/workflow paths have regression coverage.

11. Provision an Android SDK/JDK build workflow with pinned tool versions.
12. Add unit tests for queue scheduling, status transitions, and due-item selection.
13. Add tests for YouTube response parsing, missing fields, and API errors.
14. Add tests for preference migrations and fallback policy.
15. Add pipeline tests for missing images, invalid media, and cleanup.
16. Add Compose tests for loading, empty, permission-denied, and provider-error states.
17. Introduce repository interfaces around Google, Firebase, and media providers.
18. Move any remaining UI-owned domain models into shared data-layer models.
19. Remove verified dead code and duplicate helpers with usage/build checks.
20. Add formatting, lint, dependency, and static-analysis gates to CI.

### Phase 3 — Reliable creator workflow

**Acceptance:** a creator can start from a sourced/user-entered topic, inspect each generation stage, recover from failure, and find the resulting project.

21. Add a topic provenance selector: live trend, channel analytics, or user-entered.
22. Store source video IDs and trend observation timestamps with queued ideas.
23. Require a review step before converting trend titles into original scripts.
24. Add resumable generation stages and persist stage-level outcomes.
25. Add explicit retry/cancel controls with idempotent job identifiers.
26. Prevent duplicate concurrent jobs per channel and source topic.
27. Make queue state transitions explicit for generated, upload-pending, published, failed, and cancelled work.
28. Add a manual queue editor for title, category, schedule, and source attribution.
29. Add local project recovery for process death and device restart.
30. Add export/import of projects with integrity checks and clear storage controls.

### Phase 4 — YouTube analytics and channel management

**Acceptance:** every insight is derived from account-authorized data, with scope, quota, time window, and missing-data behavior made clear.

31. Add paginated channel and video retrieval with cancellation.
32. Add quota estimation and preflight checks for expensive API operations.
33. Show metric definitions and YouTube API availability constraints in context.
34. Add date-range comparisons using actual channel-owned video metrics.
35. Add per-channel analytics isolation for multi-channel accounts.
36. Add channel data refresh controls and last-successful-sync history.
37. Add playlist membership verification after writes.
38. Add safe comment-reply preview, moderation rules, and approval mode.
39. Add quota-aware comment scanning with deduplication and replay protection.
40. Add analytics export in CSV/JSON with source and timestamp metadata.

### Phase 5 — AI and media quality

**Acceptance:** media generation uses configured providers or explicit user assets; generated output is validated and failures never become success-shaped placeholders.

41. Add provider health checks and capability discovery.
42. Add user-selectable image, voice, and script providers behind typed interfaces.
43. Add content/source notes for scripts that contain factual claims.
44. Add claim-risk warnings for health, finance, crime, and current-events scripts.
45. Add narration pronunciation controls and preview playback.
46. Add subtitle timing review and manual correction.
47. Add render presets validated against target platform requirements.
48. Add media asset manifests with origin, license, and content hash.
49. Add render quality checks for missing frames, silent audio, clipping, and aspect ratio.
50. Add a provider-cost estimate and user-set spending limits.

### Phase 6 — SEO, community, and publishing quality

**Acceptance:** metadata suggestions are editable, grounded in account/topic data, and never promise unsupported performance.

51. Replace canned SEO outputs with provider-backed or transparent deterministic suggestions.
52. Add duplicate-title and near-duplicate-description checks for the channel.
53. Add title/description preview for target platform surfaces.
54. Add tag validation, length limits, and localization.
55. Add a community-post preview and approval workflow.
56. Add scheduled publishing with timezone-aware calendar display.
57. Add privacy-status and made-for-kids confirmation before upload.
58. Add resumable upload progress persistence and recovery.
59. Verify upload completion by fetching the published video from YouTube.
60. Add a post-publish checklist for visibility, playlist, captions, and metadata.

### Phase 7 — Data architecture and resilience

**Acceptance:** local/remote state has documented ownership, migrations are reversible where possible, and provider/network failures cannot corrupt user work.

61. Document a source-of-truth policy for each persisted entity.
62. Add Room schema export and tested database migrations.
63. Add encrypted storage for sensitive refresh/access tokens where applicable.
64. Add token expiry handling and user-mediated reauthorization.
65. Add bounded API retries with backoff and cancellation.
66. Add offline read-only access for saved projects and previously fetched analytics.
67. Add sync conflict detection for multi-device editing.
68. Add data retention controls for logs, transcripts, and cached media.
69. Add safe cache limits and storage-pressure handling.
70. Add observability events that exclude credentials and personal content.

### Phase 8 — Keyboard and productivity tools

**Acceptance:** local keyboard utilities are clearly distinguished from account-backed services; sensitive generated data is not inserted or stored unexpectedly.

71. Audit and label all keyboard sample/template content.
72. Add password policy controls and tests for generated-password character requirements.
73. Add privacy tests proving generated passwords are not logged or persisted and are inserted only after explicit user selection.
74. Add clear clipboard permission and retention behavior.
75. Add per-app opt-outs for sensitive keyboard features.
76. Add accessibility and IME onboarding with current Android-version guidance.
77. Add offline/online state indicators for voice and suggestion providers.
78. Add keyboard layout customization with import/export validation.
79. Add automated tests for text transforms, snippets, and cursor operations.
80. Add a privacy dashboard for keyboard data handling.

### Phase 9 — Privacy, permissions, and device tools

**Acceptance:** every sensitive capability explains why permission is requested, works only after user consent, and exposes an immediate off switch.

81. Audit each manifest permission against active feature use.
82. Add just-in-time permission rationale and recovery flows.
83. Add tests proving accessibility counters increment only after a confirmed UI action, including service interruption cases.
84. Add per-package allowlists and user-visible scope for accessibility monitoring.
85. Add VPN connection diagnostics that distinguish blocked, connecting, and connected states.
86. Remove cleartext network use where not required and document unavoidable exceptions.
87. Add privacy disclosures for device telemetry and Firebase persistence.
88. Add one-action deletion for account, analytics, logs, and local media data.
89. Add backup rules that exclude secrets and user media unless opted in.
90. Add permission revocation and app-uninstall cleanup tests.

### Phase 10 — Release readiness and advanced workflows

**Acceptance:** release builds are reproducible, signed securely, observable without sensitive data, and validated on supported Android versions/devices.

91. Add a protected release-signing workflow with no checked-in secrets.
92. Add dependency vulnerability and license checks.
93. Add Android API/device compatibility matrix tests.
94. Add instrumentation tests for login, create, upload, queue, and recovery journeys.
95. Add crash reporting with privacy-preserving opt-in and redaction.
96. Add feature flags with documented defaults and rollback behavior.
97. Add account-level automation limits and emergency stop controls.
98. Add a release checklist for OAuth verification, scopes, quotas, and privacy disclosures.
99. Add staged rollout and rollback procedures.
100. Re-run the complete inventory and data-integrity audit before each major release.
