# Phase 6 Session 2192 Handoff - FindGroup Mutation Java Trace Instrumentation Scaffold

Date: 2026-06-02
Unit of Work: UOW-2192
Status: Completed

## Startup Instructions

Future Phase 6 sessions should read:

1. `docs/csharp-port.md`
2. `docs/orchestration-rules.md`
3. `docs/parity-verification.md`
4. latest `docs/Phase-6-Session-*-Completion.md`
5. latest `docs/Phase-6-Session-*-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is historical archive material only.

Focused validation is the default. Do not run the broad .NET suite, an unfiltered project-wide test run, or a full solution build unless a documented broad-validation trigger applies. Filtered `dotnet test` commands already build the affected project and dependencies.

Use this validation decision template in future completion/handoff docs:

```text
Validation decision:
- Changed surface:
- Focused C# command:
- Focused Java/Maven command:
- Broad-validation trigger:
- Broad .NET decision:
- Why this scope is sufficient:
```

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- `PHASE-6-PROGRESS.md` remained untouched.
- Completion/handoff docs are the active progress/parity record.
- Full .NET suite/full solution build is not routine validation. Use focused test selection from `docs/orchestration-rules.md`.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post Java artifact schema/validator/instrumentation/file/directory/runbook, Java capture fixture scaffold, Java test-side instrumentation scaffold, C# trace-emitter design, C# live trace-row fixture plan, registry observation, key projection, artifact comparison preflight, trace-row readiness aggregate, comparison contracts/envelope/blockers/dry-run/result skeleton, and Java artifact capture implementation readiness exist as non-live readiness artifacts.

## UOW-2192 Summary

This UOW added `FindGroupMutationPostTraceCaptureInstrumentation`, a test-side Java hook catalog and capture-flag recorder scaffold for future `CM_FIND_GROUP` action `2` and `6` mutation-post trace capture.

The scaffold names:

- client-packet payload parsed event,
- client-packet `runImpl` entered event,
- recruitment mutation, posted-message, and refreshed-list events,
- application mutation, posted-message, and refreshed-list events,
- future trace artifact row serialization event.

It is not called by production Java code and does not write artifacts.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`
- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureInstrumentation`
- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureTest`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupMutationPostJavaInstrumentationDesignReportService` was reviewed as the source of the hook scaffold shape, but no C# code changed in this UOW.

## Validation In UOW-2192

Validation decision:

- Changed surface: focused Java test-side instrumentation scaffold and non-live design/session documentation.
- Focused C# command: not run. No C# code or C# test changed in this UOW; C# runtime validation is not applicable to a Java-test-only scaffold and docs update.
- Focused Java/Maven command:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- Broad-validation trigger: none.
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

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
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

## Files Changed In UOW-2192

- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureInstrumentation.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2192-Completion.md`
- `docs/Phase-6-Session-2192-Handoff.md`

## Commit

Commit message:

```text
[Phase 6][UOW-2192] Add find group mutation Java trace instrumentation scaffold
```
