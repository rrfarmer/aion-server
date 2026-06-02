# Phase 6 Session 2192 Completion - FindGroup Mutation Java Trace Instrumentation Scaffold

Date: 2026-06-02
Unit of Work: UOW-2192
Status: Completed

## Scope

This unit added a test-side Java instrumentation scaffold for future `CM_FIND_GROUP` action `2` and `6` mutation-post trace capture.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaInstrumentationDesignReportService.cs`

This UOW does not edit production Java source, does not call hooks from `CM_FIND_GROUP` or `FindGroupService`, does not write Java artifacts, does not serialize trace rows, does not compare Java/C# rows, and does not wire live C# `CmFindGroup` dispatch.

## Changes

- Added `FindGroupMutationPostTraceCaptureInstrumentation` under `game-server/test/com/aionemu/gameserver/services/findgroup`.
- The scaffold defines Java hook metadata for:
  - `client_packet_payload_parsed`,
  - `client_packet_run_impl_entered`,
  - `recruitment_state_mutation_recorded`,
  - `recruitment_posted_message_send_observed`,
  - `recruitment_refreshed_list_send_observed`,
  - `application_state_mutation_recorded`,
  - `application_posted_message_send_observed`,
  - `application_refreshed_list_send_observed`,
  - `trace_artifact_row_serialized`.
- Added a capture-flag-aware in-memory recorder that no-ops when `aion.findGroupMutationPost.capture` is absent and records event names only when enabled.
- Extended `FindGroupMutationPostTraceCaptureTest` to assert hook order, action `2`/`6` coverage, Java hook source names, required fields, disabled-recorder behavior, and enabled-recorder event order.
- Updated live-dispatch design notes to include the test-side instrumentation scaffold while keeping production runtime instrumentation blocked.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused Java test-side instrumentation scaffold and non-live design/session documentation.
- Focused C# command: not run. No C# code or C# test changed in this UOW; C# runtime validation is not applicable to a Java-test-only scaffold and docs update.
- Focused Java/Maven command:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, production Java source, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally because no broad trigger applied and no C# runtime surface changed.
- Why this scope is sufficient: the focused Maven command compiles the new Java scaffold and proves the fixture can exercise the hook catalog and capture-flag recorder without touching unrelated Java tests or broad .NET validation.

Result:

- Java/Maven: passed 8, failed 0, skipped 0.
- Existing unrelated Java `Unsafe`/deprecation warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureInstrumentation.java`; `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`; `Aion.GameServer.Services.FindGroupMutationPostJavaInstrumentationDesignReportService` | Test-Side Java Instrumentation Scaffold | Partial | Unit Tested | Partial Parity | The scaffold names future `readImpl`/`runImpl` trace events and required fields for actions `2` and `6`, but production `CM_FIND_GROUP` does not call it and no runtime row is captured. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureInstrumentation.java`; `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java` | Mutation-Post Hook Catalog / Recorder Scaffold | Partial | Unit Tested | Partial Parity | The scaffold preserves Java-derived mutation-before-posted-message-before-refreshed-list hook order for `addRecruitment` and `addApplication`, but production hook integration, serializer, generated artifacts, live C# rows, registry observation, and comparison execution are missing. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostTraceCaptureTest.instrumentationScaffoldListsHookPointsInJavaOrder` | Unit | `CM_FIND_GROUP.readImpl/runImpl`; `FindGroupService.addRecruitment/addApplication/showRecruitments/showApplications` review | Hook catalog order, action `2`/`6` coverage, and mutation-before-send ordering. | Focused Java scaffold assertion. | No production hooks. |
| `FindGroupMutationPostTraceCaptureTest.instrumentationScaffoldNamesJavaHookSourcesAndRequiredFields` | Unit | Java source review and C# instrumentation design report | Java source names and required trace fields for payload, mutation, posted message, and refreshed list hooks. | Focused Java source-derived metadata assertion. | No runtime rows. |
| `FindGroupMutationPostTraceCaptureTest.recorderNoOpsWhenCaptureFlagIsDisabled` | Unit | Capture-gated fixture design | Recorder produces no events when the capture flag is absent. | Focused recorder assertion. | No artifact output. |
| `FindGroupMutationPostTraceCaptureTest.recorderCapturesEventNamesWhenFlagIsEnabledWithoutWritingArtifacts` | Unit | Capture-gated fixture design | Recorder captures action `2` event names in Java mutation/send order when enabled. | Focused recorder assertion. | No production hook calls or serializer. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2 production artifacts and 2 Java test scaffold artifacts
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2 production artifacts plus the Java scaffold artifacts
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 Java artifact capture/comparison chain
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Production Java hook integration, Java serializer, generated Java artifacts, C# live fixture, live emitter, registry observation, and deterministic comparison are missing.
- The Java instrumentation scaffold is test-side metadata and recorder behavior only; it is not runtime parity evidence.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add Java trace serializer design-to-code scaffolding for mutation-post rows, still test-side and capture-gated, without writing artifact files or changing production Java send ordering.

Safe candidates:

- Add Java fixture helper methods for deterministic payload/scenario construction while keeping runtime execution disabled.
- Add production hook integration only after deciding the exact no-op hook placement and proving it does not alter `PacketSendUtility` ordering.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.

## Files Changed

- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureInstrumentation.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2192-Completion.md`
- `docs/Phase-6-Session-2192-Handoff.md`
