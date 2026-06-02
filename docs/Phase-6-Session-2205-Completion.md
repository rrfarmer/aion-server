# Phase 6 Session 2205 Completion - FindGroup Mutation Artifact-Backed Preflight Shape Inputs

Date: 2026-06-02
Unit of Work: UOW-2205
Status: Completed

## Scope

This unit updated the action `2`/`6` mutation-post comparison preflight so it can distinguish C# trace-row shape inputs from live C# trace-row evidence.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostArtifactComparisonPreflightService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostCSharpTraceRowFixtureReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostTraceRowReadinessAggregateService.cs`

This UOW does not wire live C# `CmFindGroup` dispatch, does not execute registry sends, does not add live runtime rows, and does not compare Java/C# rows.

## Changes

- Added `HasCSharpTraceRowShapeInputs` to `FindGroupMutationPostArtifactComparisonPreflightReport`.
- Added optional `FindGroupMutationPostCSharpTraceRowFixtureReport` input to `FindGroupMutationPostArtifactComparisonPreflightService.Create`.
- The preflight now records when action `2` and `6` C# fixture rows are present as shape inputs.
- Live C# rows still require both action rows to carry live boundary, executor, and registry observations.
- Added a focused artifact-backed test using checked-in Java repository artifacts plus disabled C# fixture rows:
  - Java rows are shape-valid,
  - C# shape inputs are present,
  - live C# rows remain false,
  - preflight remains `BlockedMissingLiveCSharpRows`.
- Updated live-dispatch design notes to record the new shape-input signal.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: C# service/test-only comparison preflight signal plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostArtifactComparisonPreflightServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpTraceRowFixtureReportServiceTests|FullyQualifiedName~FindGroupMutationPostTraceRowReadinessAggregateServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW.
- Broad-validation trigger: none. No live C# dispatch, shared runtime primitive, packet primitive, persistence, common world state, or broad connection side effect changed.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the changed service only adds a preflight metadata signal derived from the new C# fixture report and existing Java artifact reader; focused tests cover the edited preflight, the fixture report, and adjacent readiness aggregate.

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

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostArtifactComparisonPreflightServiceTests.Create_WithRepositoryJavaArtifactsAndDisabledCSharpFixtureRowsKeepsLiveRowsBlocked` | C# unit | Java action `2`/`6` artifact schema and disabled C# projection mappings | Repository Java artifacts plus disabled C# fixture rows are present as shape inputs, but preflight remains blocked on missing live C# rows. | Focused C# test using checked-in Java artifacts. | No live boundary capture, registry observation, or comparison execution. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2 Java source artifacts
- Total artifacts ported in this UOW: 0 new C# artifacts; 1 C# preflight service extended
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4 Java/C# trace-preparation artifacts
- Total blocked artifacts: live C# mutation-post boundary rows, registry observation, projected-row comparison, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Shape-valid Java fixture artifacts and disabled C# projection rows are not Java/C# runtime comparison evidence.
- The new C# shape-input signal is metadata only and cannot prove parity.
- Live C# trace-row emitter, registry send observation, projected-row comparison, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison remain missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a guarded live-boundary fixture skeleton for action `2`/`6` that records the intended missing executor/registry observations without sending packets, keeping production `CmFindGroup` dispatch disabled.

Safe candidates:

- Add readiness aggregate wiring that consumes `HasCSharpTraceRowShapeInputs` for clearer blocker text while still requiring live rows.
- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostArtifactComparisonPreflightService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostArtifactComparisonPreflightServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2205-Completion.md`
- `docs/Phase-6-Session-2205-Handoff.md`
