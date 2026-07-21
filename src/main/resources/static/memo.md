# Frontend Memo

## Must Read

- Frontend work for upload, chat flow, and replay boundaries must follow `docs/requirements.md`.
- Canvas data shape, AI annotation rendering, and future save/load APIs must follow `docs/canvas-protocol.md`.
- When backend canvas or replay APIs are added later, update both this memo and `todo.md` in the matching Phase 7-9 sections.

## Completed

- Built a Spring Boot static single-page frontend entry under `src/main/resources/static`.
- Added login, register, logout, token restore, and current-user status display.
- Added enabled-model loading and session sidebar refresh flow.
- Added image upload, session creation, session list, session detail, and message send/history integration with current backend APIs.
- Added question image preview plus local annotation canvas with pen, rectangle, arrow, text, undo, clear, and export JSON.
- Added AI annotation summary rendering on the same canvas so backend stub replies can already highlight or point to areas on the question image.
- Opened anonymous access for frontend static assets through Spring Security.
- Added backend canvas document and operation-log APIs: `GET/PUT /api/canvas/{sessionId}` and `POST /api/canvas/{sessionId}/operations`.
- Added `canvas_document` and `canvas_operation` persistence with initial canvas creation when a session is created.
- Switched frontend canvas persistence from local-only drafts to backend snapshot loading/saving and operation logging.
- Added backend replay API `GET /api/replay/{sessionId}` that merges chat messages, AI annotations, and canvas operations into one timeline.
- Added frontend replay UI with load, play/pause, previous, next, reset, exit, speed selection, timeline highlighting, and step-by-step rendering for messages, AI annotations, and canvas operations.
- Added backend AI annotation parser that normalizes structured annotations into `docs/canvas-protocol.md` objects.
- Saved normalized AI annotations into both `chat_message.annotation_json` and the `canvas_document` AI layer.
- Updated frontend AI annotation rendering to understand protocol `style` fields.
- Added backend model abstraction: `ModelClient`, unified model request/response records, `ModelClientRouter`, and `StubModelClient`.
- Routed message sending through the unified model client layer while preserving annotation parsing, canvas saving, and replay behavior.
- Verified Alibaba Bailian / Qwen OpenAI-compatible endpoint on 2026-07-05: `qwen-plus` text calls and `qwen-vl-plus` image calls both returned HTTP 200 through the workspace domain.
- Added backend `BailianModelClient` for Alibaba Bailian / Qwen OpenAI-compatible chat completions.
- Added `qwen-vl-plus` model seed data through Flyway migration `V4__add_bailian_qwen_model.sql`.
- Added environment-based Bailian config: `BAILIAN_API_KEY`, `BAILIAN_BASE_URL`, `BAILIAN_TIMEOUT_SECONDS`, `BAILIAN_MAX_TOKENS`, and `BAILIAN_TEMPERATURE`.
- Added custom server port config: `SERVER_PORT`, defaulting to `8080`; local overrides can also be placed in ignored `config/application.yml`.
- Added local ignored `config/application.yml` for real Bailian credentials during local development.
- Verified real `qwen-vl-plus` end-to-end flow on 2026-07-06: model list, image upload, session creation, message send, assistant message persistence, and canvas load all worked.
- Fixed real Qwen-VL response issues by using `max_tokens`, reading provider responses as UTF-8, and falling back to the first `replyText` when model JSON is malformed or truncated.
- Switched Bailian/Qwen to direct image-answer mode on 2026-07-06: no forced JSON, no guided-teacher constraint, and no required canvas annotations.
- Verified direct mode with a simple test image `2 + 3 = ?`; Qwen-VL returned the correct answer `5` and a short solution.
- Fixed placeholder-model fallback on 2026-07-06: disabled unimplemented OpenAI/Anthropic/Gemini seed models, promoted `qwen-vl-plus` to the first enabled model, and blocked silent fallback to `StubModelClient` unless the selected provider is explicitly `STUB`.
- Added streaming answer output on 2026-07-06: frontend now posts to `POST /api/sessions/{sessionId}/messages/stream`, renders SSE `delta` chunks live, and replaces the draft with the persisted assistant message on `done`.
- Enlarged the chat display and added basic answer formatting for headings, numbered steps, bold text, and inline LaTeX-style `\( ... \)` fragments.
- Added a compatibility fallback for streaming failures: if `/messages/stream` is missing or returns a non-2xx response, the frontend shows the real HTTP error and automatically sends through the existing non-stream `/messages` endpoint.
- Improved true streaming UX on 2026-07-06: the workspace now immediately shows a temporary user message and assistant draft, updates the draft incrementally from SSE `delta` events, batches high-frequency renders with `requestAnimationFrame`, and replaces drafts with persisted messages on `done`.
- Cleaned up streaming failure states so failed stream setup removes temporary drafts instead of leaving the UI stuck in a half-generating state.
- Debugged the non-streaming frontend symptom with the browser plugin on 2026-07-06: the stream endpoint was failing before deltas reached the UI, so the frontend fell back to the non-stream `/messages` endpoint.
- Fixed the stream endpoint by writing SSE directly to `HttpServletResponse`, adding no-buffer headers, registering Jackson `JavaTimeModule`, and keeping ISO date strings for streamed message payloads.
- Verified with `curl -N` that `/api/sessions/{sessionId}/messages/stream` returns `200 text/event-stream` and emits many `delta` events; verified in the browser that assistant text length grows incrementally while the card is in `streaming` state.
- Fixed long-answer truncation on 2026-07-07: frontend CSS was scrollable and did not truncate main assistant messages; the backend fallback payload was limiting user-visible `replyText` to 600 characters and local Bailian `max-tokens` was 500.
- Increased Bailian/Qwen output budget to 3000 tokens and kept full user-visible plain-text answers when falling back from unstructured model output.
- Restored guided-teacher mode on 2026-07-09: Bailian/Qwen now consumes the business prompt again, avoids first-turn final answers, asks one focused question first, and evaluates student replies before giving the next hint.
- Updated streaming draft metadata to `teacherIntent=guide_next_step` so the frontend temporary state matches the guided backend behavior.
- Added a frontend answer-mode switch on 2026-07-20: `引导模式` is the default and `直答模式` can be selected per message; the selection is persisted in local storage and sent as `mode` to both normal and streaming message APIs.
- Added backend mode propagation through `CreateMessageRequest` and `ModelChatRequest`; missing mode values safely fall back to `guided`.
- Added persisted multi-turn guidance state on 2026-07-20: `chat_session.guidance_state_json` stores the current stage, goal, hint count, stuck count, last answer status, and confidence; the state is injected into later model prompts.
- Added guardrails for guidance drift: each turn is limited to one goal, uncertain judgments ask the student for concrete work, and two consecutive non-progress turns trigger a summary/check-in instead of another guessed hint.
- Added Volcengine Ark / Doubao model support on 2026-07-21: `ArkModelClient` calls OpenAI-compatible `/api/v3/chat/completions`, sends local question images as data URLs, and exposes Ark Endpoint `ep-20260721164323-qjbgk` through `model_config`.
- Enabled Doubao streaming after verifying Ark Endpoint `ep-20260721164323-qjbgk` returns `text/event-stream`; Ark stream parsing now accepts both `delta.content` and `delta.reasoning_content`.
- Added session history deletion on 2026-07-21: the sidebar can delete a conversation through `DELETE /api/sessions/{id}`, using backend soft delete and clearing the workspace if the current session is removed.
- Clarified model selection behavior in the sidebar: choosing a model only affects newly created sessions, while existing sessions keep their original bound model.
- Re-enabled Qwen streaming in model metadata with `V10__enable_qwen_stream_support.sql` after the frontend started respecting `supportsStream`.

## Pending

- Add richer AI annotation types.
- Evaluate direct image-answer accuracy with real uploaded exam questions before re-enabling guided/annotation constraints.
- Improve mobile layout, accessibility, and keyboard shortcuts.
- Add richer conflict handling if two browser tabs edit the same canvas version at the same time.
- Continue browser-testing with several real exam images and record whether Qwen-VL direct-answer quality is acceptable for the MVP.

## Next Priority

- Restart the local backend, then re-test `C:/Users/admin/Desktop/timu.jpg` in guided mode and check whether the first answer asks a useful question instead of giving the full solution.
- Compare guided and direct answers on the same real question image, then tune the multi-turn hint policy if the model reveals too much or too little.
- Split answer evaluation from hint generation in a later iteration, with explicit `CORRECT`, `PARTIAL`, `WRONG`, and `UNCLEAR` statuses.
- Configure `ARK_API_KEY` outside the repository, then compare Doubao and Qwen-VL on the same real question image.
