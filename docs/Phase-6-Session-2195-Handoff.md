# Phase 6 Session 2195 Handoff - FindGroup Mutation Java Trace Artifact Validation Scaffold

Date: 2026-06-02
Unit of Work: UOW-2195
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post Java artifact schema/validator/instrumentation/file/directory/runbook, Java capture fixture scaffold, Java test-side instrumentation scaffold, Java test-side serializer scaffold, Java test-side artifact writer scaffold, Java test-side artifact validator scaffold, C# trace-emitter design, C# live trace-row fixture plan, registry observation, key projection, artifact comparison preflight, trace-row readiness aggregate, comparison contracts/envelope/blockers/dry-run/result skeleton, and Java artifact capture implementation readiness exist as non-live readiness artifacts.

## UOW-2195 Summary

This UOW added `FindGroupMutationPostTraceCaptureArtifactValidator`, a test-side Java fixture validator scaffold for future action `2` and `6` mutation-post trace artifacts.

The scaffold:

- reads expected action `2`/`6` files from a caller-provided artifact root,
- reports missing expected files,
- reports invalid shape or action mapping,
- reports shape-valid fixture files,
- checks Java trace source, trace name, schema version, action-specific mapping, packet types, and zero broadcast/invite counts,
- always keeps runtime comparison readiness false,
- was tested with JUnit temporary directories,
- did not create `parity-artifacts/find-group/mutation-post/java` in the repository.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`
- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureInstrumentation`
- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureSerializer`
- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureArtifactWriter`
- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureArtifactValidator`
- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureTest`

## C# Artifacts Reviewed

- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService`
- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactDirectoryReportService`

No C# code changed in this UOW.

## Validation In UOW-2195

Validation decision:

- Changed surface: focused Java test-side artifact validation scaffold plus non-live design/session documentation.
- Focused C# command: not run. No C# code or C# test changed in this UOW; C# validator/directory-reader contracts were reviewed only.
- Focused Java/Maven command:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because no broad trigger applied and no C# runtime surface changed.
- Why this scope is sufficient: the focused Maven command compiles the new Java fixture validator and proves temp-directory files can be classified as missing, shape-valid, or invalid without touching unrelated Java tests or broad .NET validation.

Result:

- Java/Maven: passed 19, failed 0, skipped 0.
- Existing unrelated Java `Unsafe`/deprecation warnings were emitted.
- Repository artifact path check: `parity-artifacts/find-group/mutation-post/java` did not exist after the test run.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureArtifactValidator.java`; `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService` | Test-Side Java Artifact Validation Scaffold | Partial | Unit Tested | Partial Parity | The scaffold classifies temp-directory fixture action `2`/`6` artifact files with Java trace source and stable mappings, but production `CM_FIND_GROUP` does not call hooks and no runtime-backed artifact row is captured. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureArtifactValidator.java`; `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureArtifactWriter.java`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactDirectoryReportService` | Mutation-Post Artifact Validation Scaffold | Partial | Unit Tested | Partial Parity | The fixture validator preserves Java-derived action mappings through temp files and keeps runtime readiness false, but production hook integration, repository artifact generation, C# artifact validation from disk, live C# rows, registry observation, and comparison execution are missing. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Production Java hook integration, runtime-backed Java artifact generation, C# repository artifact validation from disk, C# live fixture, live emitter, registry observation, and deterministic comparison are missing.
- The Java validator scaffold validates temp-directory fixture rows only; it is not runtime parity evidence.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add deterministic Java fixture scenario builders for action `2` and `6` payload/state rows so future production hook integration can reuse stable inputs instead of ad hoc sample rows.

Safe candidates:

- Add C# focused tests that feed Java fixture JSON strings into `FindGroupMutationPostJavaTraceArtifactValidatorService` if C# artifact-contract drift becomes a concern.
- Add production hook integration only after deciding exact no-op hook placement and proving it does not alter `PacketSendUtility` ordering.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.

## Files Changed In UOW-2195

- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureArtifactValidator.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2195-Completion.md`
- `docs/Phase-6-Session-2195-Handoff.md`

## Commit

Commit message:

```text
[Phase 6][UOW-2195] Add find group mutation Java trace artifact validation scaffold
```
