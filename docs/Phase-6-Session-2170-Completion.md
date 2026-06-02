# Phase 6 Session 2170 Completion - FindGroup Mutation Trace Projection

Date: 2026-06-02
Unit of Work: UOW-2170
Status: Completed

## Scope

This unit added a non-live C# export projection helper for Java `CM_FIND_GROUP` mutation-post actions `2` and `6`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- Action `2` calls `FindGroupService.addRecruitment(player, message, groupType)`, mutates recruitment state, sends `SM_SYSTEM_MESSAGE.STR_PARTY_MATCH_OFFER_PARTY_POSTED()`, then refreshes recruitments with `SM_FIND_GROUP` action `0`.
- Action `6` calls `FindGroupService.addApplication(player, message, groupType, classId, level)`, mutates application state, sends `SM_SYSTEM_MESSAGE.STR_PARTY_MATCH_SEEK_PARTY_POSTED()`, then refreshes applications with `SM_FIND_GROUP` action `4`.

This UOW does not capture live Java/C# traces, does not enable live `CM_FIND_GROUP` dispatch, and does not claim runtime/socket parity.

## Changes

- Added `FindGroupDirectPacketMutationPostBoundaryTraceSchemaService.CreateExportFromDisabledPlan`.
- Added projection result records:
  - `FindGroupDirectPacketMutationPostBoundaryTraceExportProjection`,
  - `FindGroupDirectPacketMutationPostBoundaryTraceExportProjectionStatus`.
- The projection accepts only disabled non-live action `2` and `6` composition plans.
- Created exports preserve active player facts, mutated entry id, mutation-before-direct-send evidence, posted system message recipient/id, refreshed show-list recipient/action, post-mutation visible entry ids, and zero broadcast/invite counts.
- Created exports deliberately keep `ExecutorInvokedFromBoundary=false` and `RegistrySendsObservedInOrder=false` because no live boundary execution or registry-send observation exists.
- Added focused projection tests for action `2`, action `6`, unsupported actions, and missing active player.
- Updated `FindGroupDirectPacketBoundaryTraceReadinessService` and design notes to surface the projection as non-live evidence.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused production schema/projection service, focused tests, readiness-report text, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceExportProjectionTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live projection used reviewed Java `CM_FIND_GROUP.runImpl` actions `2`/`6` plus `FindGroupService.addRecruitment/addApplication` behavior as the oracle. No narrow executable Java fixture was identified for this projection artifact.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new projection plus adjacent mutation-post schema, readiness, and disabled side-effect composition surfaces, and the filtered command builds the affected project/dependencies.

Result:

- Passed: 34
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Trace Export Projection / Runtime Comparison Readiness | Blocked | Unit Tested | Partial Parity | Action `2`/`6` C# trace export rows can be populated from disabled boundary plans, but no Java/C# trace capture or live `ProcessPacketAsync` execution has occurred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService`; `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService`; `Aion.GameServer.Services.FindGroupRecruitmentPlanService` | Trace Export Projection / Direct Packet Readiness | Partial | Unit Tested | Partial Parity | Java mutation, posted system message ordering, refreshed show-list ordering, and post-mutation race-filtered visible ids are represented in disabled C# exports. No live registry send, socket comparison, or runtime trace has executed. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupDirectPacketMutationPostBoundaryTraceExportProjectionTests.CreateExportFromDisabledPlan_ProjectsActionTwoRecruitmentMutationPost` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.addRecruitment` source review | Action `2` export fields, posted system message id `1400392`, refreshed action `0`, active player facts, mutated recruitment id, post-mutation visible ids, and false executor/registry observations. | Focused non-live projection assertion. | Does not capture runtime trace or live registry send. |
| `FindGroupDirectPacketMutationPostBoundaryTraceExportProjectionTests.CreateExportFromDisabledPlan_ProjectsActionSixApplicationMutationPost` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.addApplication` source review | Action `6` export fields, posted system message id `1400393`, refreshed action `4`, active player facts, mutated application id, post-mutation visible ids, and false executor/registry observations. | Focused non-live projection assertion. | Does not capture runtime trace or live registry send. |
| `FindGroupDirectPacketMutationPostBoundaryTraceExportProjectionTests.CreateExportFromDisabledPlan_RejectsUnsupportedShowListAction` | Unit | Java action dispatch source review | Projection rejects action `0` because the mutation-post schema supports only actions `2` and `6`. | Focused guard assertion. | Other direct-packet trace families remain separate. |
| `FindGroupDirectPacketMutationPostBoundaryTraceExportProjectionTests.CreateExportFromDisabledPlan_RejectsMissingActivePlayer` | Unit | Java `CM_FIND_GROUP.runImpl` active-player dependency | Projection rejects disabled plans without the active player required by Java. | Focused guard assertion. | Missing-active-player runtime behavior is still non-live. |
| `FindGroupDirectPacketBoundaryTraceReadinessServiceTests.CreateReport_RecordsDisabledBoundaryActionZeroAndOptInTraceEvidence` | Unit | Java direct-send source review | Readiness report surfaces the mutation-post export projection as non-live evidence. | Focused readiness-report assertion. | Report evidence only; live dispatch remains blocked. |
| `FindGroupDirectPacketBoundaryTraceReadinessServiceTests.CreateReport_RecordsNextRequiredLiveEvidence` | Unit | Java direct-send source review | Next-required evidence includes the mutation-post export projection while keeping live trace requirements blocked. | Focused readiness-report assertion. | Does not create live trace evidence. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 trace-capture gap
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- The mutation-post projection is non-live; no Java/C# trace capture, live registry send, encrypted socket capture, or real-client runtime comparison has executed.
- The projection uses disabled composition and reviewed Java source as the oracle; it does not prove `GameServerConnection` invokes the executor from the live boundary.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.

Safe candidates:

- Add a runtime comparison fixture contract row for action `2`/`6` mutation-post traces now that schema and projection exist.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.
- Add a non-live trace export projection for another direct-packet subgroup only if a stable schema already exists.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketMutationPostBoundaryTraceSchemaService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketMutationPostBoundaryTraceExportProjectionTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketBoundaryTraceReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketBoundaryTraceReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2170-Completion.md`
- `docs/Phase-6-Session-2170-Handoff.md`
