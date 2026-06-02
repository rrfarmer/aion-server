# Phase 6 Session 2205 Handoff - FindGroup Mutation Artifact-Backed Preflight Shape Inputs

Date: 2026-06-02
Unit of Work: UOW-2205
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
- `FindGroupMutationPostArtifactComparisonPreflightService` now exposes `HasCSharpTraceRowShapeInputs`, so Java shape-valid artifacts and disabled C# shape rows can be tracked together while live C# rows remain blocked.
- Shape-valid Java artifacts and shape-valid disabled C# rows are not verified parity. Live C# boundary rows, registry observation, projected-row comparison, and live dispatch remain missing.

## UOW-2205 Summary

This UOW added an artifact-backed C# shape-input signal to the mutation-post comparison preflight.

Key behavior:

- repository Java action `2`/`6` artifacts can satisfy the Java artifact reader gate,
- disabled C# action `2`/`6` fixture rows can satisfy `HasCSharpTraceRowShapeInputs`,
- disabled C# rows still do not satisfy `HasLiveCSharpTraceRows`,
- preflight remains `BlockedMissingLiveCSharpRows` until live boundary/executor/registry evidence exists.

Important notes:

- The new signal is metadata only.
- It does not execute side effects, registry sends, or comparison.
- It does not wire production `CmFindGroup` dispatch.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Reviewed

- `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService`
- `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceRowFixtureReportService`
- `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostArtifactComparisonPreflightService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostArtifactComparisonPreflightServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2205-Completion.md`
- `docs/Phase-6-Session-2205-Handoff.md`

## Validation In UOW-2205

Validation decision:

- Changed surface: C# service/test-only comparison preflight signal plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostArtifactComparisonPreflightServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpTraceRowFixtureReportServiceTests|FullyQualifiedName~FindGroupMutationPostTraceRowReadinessAggregateServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because filtered tests built the affected C# project/dependencies and no broad trigger applied.
- Why this scope is sufficient: the changed service only adds a preflight metadata signal derived from the C# fixture report and existing Java artifact reader; focused tests cover the edited preflight, the fixture report, and adjacent readiness aggregate.

Result:

- Focused C# command: passed 17, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed. Git emitted line-ending normalization warnings for touched documentation files on Windows.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService`; `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceRowFixtureReportService` | Client Packet Boundary / Preflight | Partial | Unit Tested | Partial Parity | Preflight can now show that Java artifact rows and C# disabled projection shape rows are both present. This does not satisfy live C# rows because production `CmFindGroup` dispatch, executor invocation, and registry observation remain missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService`; `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceRowFixtureReportService` | Service Mutation / Preflight | Partial | Unit Tested | Partial Parity | Action `2` shape input tracks recruitment system message `1400392` and refreshed action `0`; no live send ordering or runtime comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService`; `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceRowFixtureReportService` | Service Mutation / Preflight | Partial | Unit Tested | Partial Parity | Action `6` shape input tracks application system message `1400393` and refreshed action `4`; no live send ordering or runtime comparison exists. |
| Repository artifacts under `parity-artifacts/find-group/mutation-post/java` | `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService` | Golden Fixture Artifact / Preflight Input | Partial | Unit Tested | Partial Parity | Checked-in Java artifacts can satisfy the Java artifact reader gate while disabled C# shape rows satisfy only the new shape-input signal. Runtime readiness remains blocked on live C# rows and registry observation. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Shape-valid Java fixture artifacts and disabled C# projection rows are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, registry observation, projected-row comparison, comparison execution, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a guarded live-boundary fixture skeleton for action `2`/`6` that records the intended missing executor/registry observations without sending packets, keeping production `CmFindGroup` dispatch disabled.

Safe candidates:

- Add readiness aggregate wiring that consumes `HasCSharpTraceRowShapeInputs` for clearer blocker text while still requiring live rows.
- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.

## Commit

Commit message:

```text
[Phase 6][UOW-2205] Track find group mutation preflight shape inputs
```
