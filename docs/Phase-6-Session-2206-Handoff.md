# Phase 6 Session 2206 Handoff - FindGroup Mutation Guarded Boundary Fixture Skeleton

Date: 2026-06-02
Unit of Work: UOW-2206
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
- `PHASE-6-PROGRESS.md` remained untouched and should not be reopened for normal startup.
- Completion/handoff docs are the active progress/parity record.
- Broad .NET validation is not routine. Use focused test selection from `docs/orchestration-rules.md`.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Checked-in Java action `2`/`6` mutation-post artifacts exist under `parity-artifacts/find-group/mutation-post/java` and are shape-valid to the C# artifact reader.
- `FindGroupMutationPostCSharpTraceRowFixtureReportService` classifies disabled action `2`/`6` C# projection rows as shape-valid but non-live.
- `FindGroupMutationPostArtifactComparisonPreflightService` exposes `HasCSharpTraceRowShapeInputs`.
- `FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService` now records the explicit trace guard, production dispatch guard, action `2`/`6` scenario requirements, and missing executor/registry observations for a future guarded boundary fixture.
- Shape-valid Java artifacts, shape-valid disabled C# rows, and the guarded fixture skeleton are not verified parity. Live C# boundary rows, registry observation, projected-row comparison, and live dispatch remain missing.

## UOW-2206 Summary

This UOW added a guarded live-boundary fixture skeleton service for mutation-post action `2`/`6` rows.

Key behavior:

- requires explicit trace guard `AION_FIND_GROUP_MUTATION_POST_TRACE_GUARD`,
- keeps production `GameServerConnection.ProcessPacketAsync` `CmFindGroup` dispatch disabled,
- records action `2` and `6` scenario requirements,
- records missing executor and registry observations as blockers,
- can carry repository Java artifact and disabled C# shape-input preflight state without marking rows live.

Important notes:

- The skeleton is a report only.
- It does not execute side effects, registry sends, or comparison.
- It does not wire production `CmFindGroup` dispatch.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Reviewed

- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.FindGroupMutationPostCSharpLiveTraceRowFixturePlanService`
- `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2206-Completion.md`
- `docs/Phase-6-Session-2206-Handoff.md`

## Validation In UOW-2206

Validation decision:

- Changed surface: C# service/test-only guarded fixture skeleton plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpLiveTraceRowFixturePlanServiceTests|FullyQualifiedName~FindGroupMutationPostArtifactComparisonPreflightServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because filtered tests built the affected C# project/dependencies and no broad trigger applied.
- Why this scope is sufficient: the added service is a non-live fixture skeleton and depends on existing fixture-plan/preflight services; focused tests cover the skeleton plus both adjacent surfaces.

Result:

- Focused C# command: passed 20, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed. Git emitted line-ending normalization warnings for touched documentation files on Windows.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService`; `Aion.GameServer.Network.Aion.GameServerConnection` | Client Packet Boundary / Fixture Skeleton | Partial | Unit Tested | Partial Parity | Skeleton records a future guarded boundary fixture for actions `2`/`6` while keeping production `ProcessPacketAsync` dispatch disabled. No live boundary rows, executor invocation, registry observation, or comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService` | Service Mutation / Fixture Skeleton | Partial | Unit Tested | Partial Parity | Action `2` scenario names Java recruitment mutation ordering, posted system message `1400392`, and refreshed action `0`. Evidence is planning/skeleton only. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService` | Service Mutation / Fixture Skeleton | Partial | Unit Tested | Partial Parity | Action `6` scenario names Java application mutation ordering, posted system message `1400393`, and refreshed action `4`. Evidence is planning/skeleton only. |
| Repository artifacts under `parity-artifacts/find-group/mutation-post/java` | `Aion.GameServer.Services.FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService`; `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService` | Golden Fixture Artifact / Skeleton Handoff | Partial | Unit Tested | Partial Parity | Skeleton can carry shape-valid Java artifact and disabled C# shape-input state through preflight, but live rows remain false. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Shape-valid Java fixture artifacts, disabled C# projection rows, and the guarded fixture skeleton are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, registry observation, projected-row comparison, comparison execution, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Next Recommended Unit of Work

Next sequential task:

- Add readiness aggregate wiring that consumes the guarded boundary fixture skeleton and `HasCSharpTraceRowShapeInputs` for clearer blocker text while still requiring live rows, executor observation, and registry observation.

Safe candidates:

- Add a guarded fixture result contract for action `2`/`6` that can later hold real boundary rows without sending packets by default.
- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.

## Commit

Commit message:

```text
[Phase 6][UOW-2206] Add find group mutation guarded boundary fixture skeleton
```
