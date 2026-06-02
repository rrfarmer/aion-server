# Phase 6 Session 2168 Completion - FindGroup Show-List Trace Export Projection

Date: 2026-06-02
Unit of Work: UOW-2168
Status: Completed

## Scope

This unit added a non-live projection helper that populates the existing action `0`/`4` show-list trace schema from disabled `CM_FIND_GROUP` boundary plans.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- Action `0` calls `FindGroupService.showRecruitments(player)` and sends one `SM_FIND_GROUP` action `0` packet to the triggering player.
- Action `4` calls `FindGroupService.showApplications(player)` and sends one `SM_FIND_GROUP` action `4` packet to the triggering player.
- Both branches filter entries by active player race, materialize the list, and do not world-broadcast or dispatch invites.

This UOW does not capture Java/C# runtime traces, does not enable live `CM_FIND_GROUP` dispatch, and does not observe live registry sends.

## Changes

- Added `FindGroupDirectPacketShowListBoundaryTraceSchemaService.CreateExportFromDisabledPlan`.
- Added projection result/status records:
  - `FindGroupDirectPacketShowListBoundaryTraceExportProjection`,
  - `FindGroupDirectPacketShowListBoundaryTraceExportProjectionStatus`.
- The helper projects schema version `1` C# trace exports for disabled action `0`/`4` plans.
- Projected fields include boundary acceptance, active player object id/race, server epoch seconds, list kind, visible entry ids, direct packet recipient/type/action, and zero broadcast/invite counts.
- Executor and registry observation fields remain `false` because the helper does not invoke live or opt-in sends.
- Unsupported actions, missing active player, missing client action plan, live-side-effect plans, unexpected side-effect shapes, and missing show-list plans are rejected with explicit statuses.
- Updated `FindGroupDirectPacketBoundaryTraceReadinessService` to surface the export projection as non-live evidence.
- Updated `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused production schema/projection helper, focused tests, readiness-report text, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupDirectPacketShowListBoundaryTraceExportProjectionTests|FullyQualifiedName~FindGroupDirectPacketShowListBoundaryTraceSchemaServiceTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live projection used reviewed Java `CM_FIND_GROUP.runImpl` actions `0`/`4` plus `FindGroupService.showRecruitments/showApplications` behavior as the oracle. No narrow executable Java fixture was identified for this readiness artifact.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the projection helper, stable schema fields, direct-packet readiness report, and adjacent disabled boundary composition evidence, and the filtered command built the affected project/dependencies.

Result:

- Passed: 34
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupDirectPacketShowListBoundaryTraceSchemaService` | Trace Export Projection / Runtime Comparison Readiness | Blocked | Unit Tested | Partial Parity | Action `0`/`4` C# trace export rows can be populated from disabled boundary plans, but no Java/C# trace capture or live `ProcessPacketAsync` execution has occurred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupDirectPacketShowListBoundaryTraceSchemaService`; `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService`; `Aion.GameServer.Services.FindGroupRecruitmentPlanService` | Trace Export Projection / Direct Packet Readiness | Partial | Unit Tested | Partial Parity | Java show-list filtering and direct-send comparison fields are represented in disabled C# exports. No live registry send, socket comparison, or runtime trace has executed. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupDirectPacketShowListBoundaryTraceExportProjectionTests.CreateExportFromDisabledPlan_ProjectsActionZeroRecruitmentShowList` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.showRecruitments` source review | Action `0` export fields, race-filtered visible recruitment ids, active player facts, direct recipient, and false executor/registry observations. | Focused non-live projection assertion. | Does not capture runtime trace or live registry send. |
| `FindGroupDirectPacketShowListBoundaryTraceExportProjectionTests.CreateExportFromDisabledPlan_ProjectsActionFourApplicationShowList` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.showApplications` source review | Action `4` export fields, race-filtered visible application ids, active player facts, direct recipient, and false executor/registry observations. | Focused non-live projection assertion. | Does not capture runtime trace or live registry send. |
| `FindGroupDirectPacketShowListBoundaryTraceExportProjectionTests.CreateExportFromDisabledPlan_RejectsUnsupportedMutationAction` | Unit | Java action dispatch source review | Projection rejects action `2` because the show-list schema supports only actions `0` and `4`. | Focused guard assertion. | Mutation-post traces still need their own schema/export shape. |
| `FindGroupDirectPacketShowListBoundaryTraceExportProjectionTests.CreateExportFromDisabledPlan_RejectsMissingActivePlayer` | Unit | Java `CM_FIND_GROUP.runImpl` active-player dependency | Projection rejects disabled plans without the active player required by Java. | Focused guard assertion. | Missing-active-player runtime behavior is still non-live. |
| `FindGroupDirectPacketBoundaryTraceReadinessServiceTests.CreateReport_RecordsDisabledBoundaryActionZeroAndOptInTraceEvidence` | Unit | Java direct-send source review | Readiness report includes show-list export projection evidence. | Focused readiness-report assertion. | Report evidence only; live dispatch remains blocked. |
| `FindGroupDirectPacketBoundaryTraceReadinessServiceTests.CreateReport_RecordsNextRequiredLiveEvidence` | Unit | Java direct-send source review | Next-required evidence mentions the export projection helper and keeps live trace requirements blocked. | Focused readiness-report assertion. | Does not create live trace evidence. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 trace-capture gap
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- The export projection helper is non-live; no Java/C# trace capture, live registry send, encrypted socket capture, or real-client runtime comparison has executed.
- Mutation-post trace exports, world-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a trace schema/export DTO for action `2`/`6` mutation-post boundary traces.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add a non-live export projection helper for action `2`/`6` mutation-post disabled boundary plans after the schema exists.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketShowListBoundaryTraceSchemaService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketShowListBoundaryTraceExportProjectionTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketBoundaryTraceReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketBoundaryTraceReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2168-Completion.md`
- `docs/Phase-6-Session-2168-Handoff.md`
