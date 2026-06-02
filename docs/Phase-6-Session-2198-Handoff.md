# Phase 6 Session 2198 Handoff - FindGroup Mutation Java Hook Placement Preflight

Date: 2026-06-02
Unit of Work: UOW-2198
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post Java artifact schema/validator/instrumentation/file/directory/runbook, Java capture fixture scaffold, Java test-side instrumentation scaffold, Java test-side scenario builder, Java test-side serializer scaffold, Java test-side artifact writer scaffold, Java test-side artifact validator scaffold, Java test-side hook-placement preflight, C# deterministic Java fixture validator alignment, C# trace-emitter design, C# live trace-row fixture plan, registry observation, key projection, artifact comparison preflight, trace-row readiness aggregate, comparison contracts/envelope/blockers/dry-run/result skeleton, and Java artifact capture implementation readiness exist as non-live readiness artifacts.

## UOW-2198 Summary

This UOW added `FindGroupMutationPostTraceCaptureHookPlacementPreflight`, a Java test-side source-order preflight for future action `2` and `6` mutation-post trace hook placement.

The preflight confirms:

- action `2` has `recruitments.put(...)` before `STR_PARTY_MATCH_OFFER_PARTY_POSTED`, followed by `showRecruitments(player)`, with `showRecruitments` sending `new SM_FIND_GROUP(0, recruitments)`;
- action `6` has `applications.put(...)` before `STR_PARTY_MATCH_SEEK_PARTY_POSTED`, followed by `showApplications(player)`, with `showApplications` sending `new SM_FIND_GROUP(4, applications)`;
- production hook integration remains false.

This is source-inspection evidence only. It is not runtime Java trace evidence, not repository artifact evidence, and not Java/C# comparison evidence.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`
- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureInstrumentation`
- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureTest`

## Artifacts Touched

- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureHookPlacementPreflight.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2198-Completion.md`
- `docs/Phase-6-Session-2198-Handoff.md`

No C# files changed.

## Validation In UOW-2198

Validation decision:

- Changed surface: Java test-only hook-placement preflight plus non-live design/session documentation.
- Focused C# command: not run; no C# files changed.
- Focused Java/Maven command:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because no C# files changed and no broad trigger applied.
- Why this scope is sufficient: the targeted Maven command compiles and runs the Java fixture class that owns the new preflight and directly inspects the Java source-of-truth statements under review.

Result:

- Final Java/Maven result: passed 22, failed 0, skipped 0. Existing Java `Unsafe` and deprecation warnings were emitted from unrelated golden-test helpers during test compilation.
- Initial focused Maven run failed with `NoSuchFileException` for the `FindGroupService.java` source path; the preflight now resolves both module-base and repository-root layouts.
- `git diff --check`: passed. Git emitted line-ending normalization warnings for touched files on Windows.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | Java test-side `FindGroupMutationPostTraceCaptureHookPlacementPreflight`; future C# mutation-post trace comparison chain | Hook Placement Preflight | Partial | Unit Tested | Partial Parity | The preflight confirms current Java source order for action `2`/`6` mutation before posted system message before refresh call, with expected refreshed-list send statements present. This is not runtime evidence; production hooks, Java artifact generation, C# live rows, registry observation, and comparison remain missing. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | Java test-side mutation-post fixture scaffold; future C# live trace row fixture | Mutation-Post Trace Preparation | Partial | Unit Tested | Partial Parity | Existing fixture tests continue to cover action `2`/`6` metadata and artifact targets. Live dispatch and runtime comparison remain blocked. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Production Java hook integration, runtime-backed Java artifact generation, C# repository artifact validation from disk, C# live fixture, live emitter, registry observation, and deterministic comparison are missing.
- The source-order preflight is not runtime parity evidence.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add no-op production Java hook integration behind the existing capture flag only after reviewing the preflight insertion points, proving default execution is unchanged, and keeping artifact output disabled unless explicitly capture-enabled.

Safe candidates:

- Add a focused Java fixture test proving disabled no-op hooks do not write repository artifact files.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add fixture-side payload byte construction only if future Java packet parser capture needs deterministic raw packet bytes.

## Commit

Commit message:

```text
[Phase 6][UOW-2198] Add find group mutation Java hook placement preflight
```
